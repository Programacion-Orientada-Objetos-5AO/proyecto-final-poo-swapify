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

/**
 * Lógica de negocio para gestionar publicaciones y su contenido multimedia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicacionService {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final long MAX_IMAGE_BYTES = 5_000_000L;

    private final PublicacionRepository publicacionRepository;
    private final PublicacionMapper publicacionMapper;
    private final ar.edu.huergo.swapify.repository.security.UsuarioRepository usuarioRepository;

    /**
     * Crea una publicación tomando los datos del DTO y asociándola al usuario
     * autenticado.
     */
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
                byte[] bytes = archivo.getBytes();
                if (bytes.length == 0) {
                    throw new IllegalArgumentException("La imagen es obligatoria");
                }
                guardarImagen(p, bytes, archivo.getContentType());
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo leer la imagen: " + e.getMessage(), e);
            }
        } else if (dto.getImagenBase64() != null && !dto.getImagenBase64().isBlank()) {
            String base64Data = dto.getImagenBase64();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
            }

            byte[] bytes;
            try {
                bytes = decodificarBase64(base64Data);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("La imagen está dañada o tiene un formato inválido");
            }

            if (bytes.length == 0) {
                throw new IllegalArgumentException("La imagen es obligatoria");
            }

            guardarImagen(p, bytes, dto.getImagenContentType());
        } else {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
        Publicacion guardada = publicacionRepository.save(p);
        prepararPublicacionParaLectura(guardada);
        return guardada;
    }

    /**
     * Devuelve todas las publicaciones ordenadas por fecha descendente.
     */
    @Transactional(readOnly = true)
    public List<Publicacion> listarTodas() {
        List<Publicacion> publicaciones = publicacionRepository.findAllByOrderByFechaPublicacionDesc();
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    /**
     * Obtiene una publicación por identificador asegurando que esté lista para
     * ser mostrada.
     */
    @Transactional(readOnly = true)
    public Publicacion obtenerPorId(Long id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
        prepararPublicacionParaLectura(publicacion);
        return publicacion;
    }

    /**
     * Listado de publicaciones generadas durante una fecha específica.
     */
    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesDeFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        List<Publicacion> publicaciones = publicacionRepository.findByFechaPublicacionBetween(inicio, fin);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    /**
     * Calcula la suma de precios referenciales de las publicaciones creadas en
     * una fecha.
     */
    @Transactional(readOnly = true)
    public BigDecimal sumaPreciosEnFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(inicio, fin);
        return (suma != null) ? suma : BigDecimal.ZERO;
    }

    /**
     * Recupera las publicaciones pertenecientes a un usuario específico.
     */
    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesPorUsuario(Long usuarioId) {
        List<Publicacion> publicaciones = publicacionRepository.findByUsuarioId(usuarioId);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    /**
     * Lista las publicaciones del usuario autenticado ordenadas por fecha.
     */
    @Transactional(readOnly = true)
    public List<Publicacion> listarPorUsuario(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        List<Publicacion> publicaciones = publicacionRepository.findByUsuarioUsernameOrderByFechaPublicacionDesc(username);
        publicaciones.forEach(this::prepararPublicacionParaLectura);
        return publicaciones;
    }

    /**
     * Elimina una publicación validando que pertenezca al usuario indicado.
     */
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

    /**
     * Procesa y almacena la imagen asociada a una publicación aplicando
     * validaciones y optimizaciones.
     */
    private void guardarImagen(Publicacion publicacion, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            publicacion.limpiarImagen();
            return;
        }

        String tipoNormalizado = normalizarContentType(contentType);
        byte[] originales = bytes;

        BufferedImage original = null;
        try {
            original = leerImagen(bytes);
            if (original == null) {
                throw new IllegalArgumentException("La imagen está dañada o tiene un formato inválido");
            }

            ImagenProcesada procesada = optimizarImagen(bytes, tipoNormalizado, original);
            byte[] optimizadas = procesada.datos();
            if (optimizadas != null && optimizadas.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
            }
            publicacion.setImagen(optimizadas);
            publicacion.setImagenContentType(procesada.contentType());
        } catch (OutOfMemoryError e) {
            log.error("Sin memoria para procesar la imagen ({} bytes)", bytes != null ? bytes.length : -1, e);
            throw new IllegalArgumentException("La imagen es demasiado grande para procesarla. Reducila e intentá nuevamente.");
        } catch (IOException e) {
            throw new IllegalArgumentException("La imagen está dañada o tiene un formato inválido", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo optimizar la imagen, se almacenará el archivo original", e);
            byte[] copia = originales != null ? originales.clone() : null;
            if (copia != null && copia.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
            }
            publicacion.setImagen(copia);
            publicacion.setImagenContentType(tipoNormalizado);
        }
    }

    /**
     * Completa los datos derivados de una publicación para ser mostrados en
     * vistas o respuestas.
     */
    private void prepararPublicacionParaLectura(Publicacion publicacion) {
        if (publicacion == null) {
            return;
        }

        if (publicacion.getUsuario() != null) {
            publicacion.getUsuario().getUsername();
        }

        byte[] imagen = publicacion.getImagen();
        if (imagen != null && imagen.length > 0) {
            publicacion.setImagen(imagen);

            String base64 = java.util.Base64.getEncoder().encodeToString(imagen);
            publicacion.setImagenBase64(base64);

            String contentType = publicacion.getImagenContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }
            publicacion.setImagenDataUri("data:" + contentType + ";base64," + base64);
        } else {
            publicacion.setImagen(null);
            publicacion.setImagenBase64(null);
            publicacion.setImagenDataUri(null);
        }
    }

    /**
     * Decodifica una cadena Base64 aceptando variantes con espacios o saltos de
     * línea.
     */
    private byte[] decodificarBase64(String base64Data) {
        if (base64Data == null) {
            return new byte[0];
        }

        StringBuilder limpio = new StringBuilder(base64Data.length());
        for (int i = 0; i < base64Data.length(); i++) {
            char c = base64Data.charAt(i);
            if (c == ' ') {
                limpio.append('+');
                continue;
            }
            if (!Character.isWhitespace(c)) {
                limpio.append(c);
            }
        }

        if (limpio.length() == 0) {
            return new byte[0];
        }

        return Base64.getMimeDecoder().decode(limpio.toString());
    }

    /**
     * Normaliza el content type recibido a valores aceptados por el backend.
     */
    private String normalizarContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }
        if (contentType.equalsIgnoreCase("image/jpg")) {
            return "image/jpeg";
        }
        return contentType.toLowerCase();
    }

    /**
     * Aplica escalado y compresión para mantener el tamaño de la imagen dentro
     * de los límites permitidos.
     */
    private ImagenProcesada optimizarImagen(byte[] data, String contentType, BufferedImage original) throws IOException {
        if (data == null || data.length == 0) {
            return new ImagenProcesada(data, normalizarContentType(contentType));
        }

        byte[] procesada = data;
        BufferedImage imagenBase = original;

        EscaladoResult escalado = escalarSiEsNecesario(original, contentType);
        if (escalado != null) {
            procesada = escalado.datos();
            imagenBase = escalado.imagen();
        }

        if (procesada.length > MAX_IMAGE_BYTES) {
            procesada = recomprimirComoJpeg(imagenBase);
            return new ImagenProcesada(procesada, "image/jpeg");
        }

        return new ImagenProcesada(procesada, normalizarContentType(contentType));
    }

    /**
     * Escala la imagen cuando supera las dimensiones máximas permitidas.
     */
    private EscaladoResult escalarSiEsNecesario(BufferedImage original, String contentType) throws IOException {
        int width = original.getWidth();
        int height = original.getHeight();
        int maxDimension = Math.max(width, height);
        if (maxDimension <= MAX_IMAGE_DIMENSION) {
            return null;
        }

        double scale = (double) MAX_IMAGE_DIMENSION / maxDimension;
        int newWidth = (int) Math.round(width * scale);
        int newHeight = (int) Math.round(height * scale);

        boolean formatoConTransparencia = contentType != null && (contentType.contains("png") || contentType.contains("gif"));
        boolean requiereTransparencia = formatoConTransparencia && original.getColorModel() != null
                && original.getColorModel().hasAlpha();
        int imageType = requiereTransparencia ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

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
                return null;
            }
            return new EscaladoResult(baos.toByteArray(), escalada);
        }
    }

    private byte[] recomprimirComoJpeg(BufferedImage image) throws IOException {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgb.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(image, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        java.util.Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            try (ByteArrayOutputStream fallback = new ByteArrayOutputStream()) {
                if (!ImageIO.write(rgb, "jpg", fallback)) {
                    return convertirBufferedImageABytes(image, "jpg");
                }
                return fallback.toByteArray();
            }
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.8f);
            }
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.flush();
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private byte[] convertirBufferedImageABytes(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, baos)) {
                return new byte[0];
            }
            return baos.toByteArray();
        }
    }

    private BufferedImage leerImagen(byte[] data) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            return ImageIO.read(input);
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

    private static class EscaladoResult {
        private final byte[] datos;
        private final BufferedImage imagen;

        private EscaladoResult(byte[] datos, BufferedImage imagen) {
            this.datos = datos;
            this.imagen = imagen;
        }

        public byte[] datos() {
            return datos;
        }

        public BufferedImage imagen() {
            return imagen;
        }
    }
}
