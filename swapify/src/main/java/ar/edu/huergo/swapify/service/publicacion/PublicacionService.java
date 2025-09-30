package ar.edu.huergo.swapify.service.publicacion;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicacionService {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final long MAX_IMAGE_BYTES = 5_000_000L;

    private final PublicacionRepository publicacionRepository;
    private final PublicacionMapper publicacionMapper;
    private final ar.edu.huergo.swapify.repository.security.UsuarioRepository usuarioRepository;

    @Transactional
    public Publicacion crearPublicacion(CrearPublicacionDTO dto, ar.edu.huergo.swapify.entity.security.Usuario usuario) {
        if (dto == null) throw new IllegalArgumentException("Datos de publicación inválidos");
        var managedUsuario = usuarioRepository.findByUsername(usuario.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Publicacion p = publicacionMapper.toEntity(dto);
        p.setUsuario(managedUsuario);
        p.setFechaPublicacion(LocalDateTime.now());

        MultipartFile archivo = dto.getImagenArchivo();
        if (archivo != null && !archivo.isEmpty()) {
            try {
                String contentType = normalizarContentType(archivo.getContentType());
                byte[] bytes = archivo.getBytes();
                guardarImagen(p, bytes, contentType);
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo leer la imagen: " + e.getMessage(), e);
            }
        } else if (dto.getImagenBase64() != null && !dto.getImagenBase64().isBlank()) {
            try {
                String base64Data = dto.getImagenBase64();
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
                }
                String contentType = normalizarContentType(dto.getImagenContentType());
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                guardarImagen(p, bytes, contentType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Formato de imagen inválido", e);
            }
        }
        Publicacion guardada = publicacionRepository.save(p);
        prepararPublicacionParaLectura(guardada);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Publicacion> listarTodas() {
        List<Publicacion> publicaciones = publicacionRepository.findAllByOrderByFechaPublicacionDesc();
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    @Transactional(readOnly = true)
    public Publicacion obtenerPorId(Long id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
        prepararPublicacionParaLectura(publicacion);
        return publicacion;
    }

    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesDeFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        List<Publicacion> publicaciones = publicacionRepository.findByFechaPublicacionBetween(inicio, fin);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    @Transactional(readOnly = true)
    public BigDecimal sumaPreciosEnFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(inicio, fin);
        return (suma != null) ? suma : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesPorUsuario(Long usuarioId) {
        List<Publicacion> publicaciones = publicacionRepository.findByUsuarioId(usuarioId);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    @Transactional(readOnly = true)
    public List<Publicacion> listarPorUsuario(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        List<Publicacion> publicaciones = publicacionRepository.findByUsuarioUsernameOrderByFechaPublicacionDesc(username);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    @Transactional
    public void eliminarPublicacion(Long publicacionId, String username) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));

        if (publicacion.getUsuario() == null || publicacion.getUsuario().getUsername() == null
                || !publicacion.getUsuario().getUsername().equals(username)) {
            throw new AccessDeniedException("No tenés permiso para eliminar esta publicación");
        }

        publicacionRepository.delete(publicacion);
    }

    private void guardarImagen(Publicacion publicacion, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            publicacion.limpiarImagen();
            return;
        }

        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
        }

        try {
            ImagenProcesada procesada = optimizarImagen(bytes, contentType);
            publicacion.setImagen(procesada.datos());
            publicacion.setImagenContentType(procesada.contentType());
        } catch (Exception e) {
            log.warn("No se pudo optimizar la imagen, se almacenará el archivo original", e);
            publicacion.setImagen(bytes);
            publicacion.setImagenContentType(normalizarContentType(contentType));
        }
    }

    private void prepararPublicacionParaLectura(Publicacion publicacion) {
        if (publicacion == null) {
            return;
        }

        if (publicacion.getUsuario() != null) {
            // Forzamos la inicialización de los datos básicos del usuario
            publicacion.getUsuario().getUsername();
        }

        if (publicacion.getImagen() != null) {
            // getImagen realiza una copia defensiva; volvemos a setearla para
            // asegurarnos de contar con un array independiente de la sesión JPA.
            publicacion.setImagen(publicacion.getImagen());
        }
    }

    private String normalizarContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }
        if (contentType.equalsIgnoreCase("image/jpg")) {
            return "image/jpeg";
        }
        return contentType.toLowerCase();
    }

    private ImagenProcesada optimizarImagen(byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            return new ImagenProcesada(data, contentType);
        }

        try {
            byte[] ajustada = escalarSiEsNecesario(data, contentType);
            if (ajustada == null) {
                ajustada = data;
            }
            String tipoActual = contentType;
            if (ajustada.length > MAX_IMAGE_BYTES) {
                ajustada = recomprimirComoJpeg(ajustada);
                tipoActual = "image/jpeg";
            }
            return new ImagenProcesada(ajustada, normalizarContentType(tipoActual));
        } catch (IOException e) {
            log.debug("Fallo al optimizar imagen, se usará el archivo original", e);
            return new ImagenProcesada(data, normalizarContentType(contentType));
        }
    }

    private byte[] escalarSiEsNecesario(byte[] data, String contentType) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            BufferedImage original = ImageIO.read(input);
            if (original == null) {
                return data;
            }

            int width = original.getWidth();
            int height = original.getHeight();
            int maxDimension = Math.max(width, height);
            if (maxDimension <= MAX_IMAGE_DIMENSION) {
                return data;
            }

            double scale = (double) MAX_IMAGE_DIMENSION / maxDimension;
            int newWidth = (int) Math.round(width * scale);
            int newHeight = (int) Math.round(height * scale);

            boolean soportaTransparencia = contentType != null && (contentType.contains("png") || contentType.contains("gif"));
            int imageType = soportaTransparencia ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

            BufferedImage escalada = new BufferedImage(newWidth, newHeight, imageType);
            Graphics2D g2d = escalada.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
            } finally {
                g2d.dispose();
            }

            String format = obtenerFormatoDesdeContentType(contentType);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (!ImageIO.write(escalada, format, baos)) {
                    return data;
                }
                return baos.toByteArray();
            }
        }
    }

    private byte[] recomprimirComoJpeg(byte[] data) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            BufferedImage original = ImageIO.read(input);
            if (original == null) {
                return data;
            }

            BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgb.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.drawImage(original, 0, 0, null);
            } finally {
                g2d.dispose();
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").hasNext()
                    ? ImageIO.getImageWritersByFormatName("jpg").next()
                    : null;
            if (writer == null) {
                try (ByteArrayOutputStream fallback = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(rgb, "jpg", fallback)) {
                        return data;
                    }
                    return fallback.toByteArray();
                }
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.8f);
                }
                writer.write(null, new IIOImage(rgb, null, null), param);
                writer.dispose();
                return baos.toByteArray();
            }
        }
    }

    private String obtenerFormatoDesdeContentType(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        if (contentType.contains("png")) {
            return "png";
        }
        if (contentType.contains("gif")) {
            return "gif";
        }
        if (contentType.contains("bmp")) {
            return "bmp";
        }
        return "jpg";
    }

    private static class ImagenProcesada {
        private final byte[] datos;
        private final String contentType;

        private ImagenProcesada(byte[] datos, String contentType) {
            this.datos = datos;
            this.contentType = contentType;
        }

        public byte[] datos() {
            return datos;
        }

        public String contentType() {
            return contentType;
        }
    }
}
