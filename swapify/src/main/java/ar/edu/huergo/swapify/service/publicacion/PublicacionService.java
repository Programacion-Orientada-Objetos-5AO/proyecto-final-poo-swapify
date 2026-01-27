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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.EstadoPublicacion;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.publicacion.PublicacionImagen;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import ar.edu.huergo.swapify.repository.publicacion.OfertaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Lógica de negocio para gestionar publicaciones y su contenido multimedia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicacionService {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final long MAX_IMAGE_BYTES = 5_000_000L;
    private static final Pattern BASE64_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern BASE64_ALLOWED = Pattern.compile("^[A-Za-z0-9+/]*={0,2}$");

    private final PublicacionRepository publicacionRepository;
    private final OfertaRepository ofertaRepository;
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
        p.setEstado(EstadoPublicacion.ACTIVA);
        p.setOficial(usuarioEsAdmin(managedUsuario));

        List<ImagenEntrada> imagenes = prepararEntradasImagen(dto);
        if (imagenes.isEmpty()) {
            throw new IllegalArgumentException("Debés adjuntar al menos una imagen");
        }
        p.limpiarImagenes();
        p.setLegacyImagen(null);
        p.setLegacyImagenContentType(null);
        int orden = 0;
        for (ImagenEntrada entrada : imagenes) {
            PublicacionImagen imagen = procesarImagenPublicacion(entrada.datos(), entrada.contentType(), orden++);
            p.agregarImagen(imagen);
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
     * Devuelve únicamente las publicaciones que continúan aceptando ofertas
     * para su exhibición pública.
     */
    @Transactional(readOnly = true)
    public List<Publicacion> listarDisponibles() {
        return publicacionRepository.findAllByOrderByFechaPublicacionDesc().stream()
                .peek(this::prepararPublicacionParaLectura)
                .filter(publicacion -> {
                    if (publicacion.getEstado() == null) {
                        publicacion.setEstado(EstadoPublicacion.ACTIVA);
                    }
                    return publicacion.estaActiva();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Publicacion> buscarDisponibles(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return listarDisponibles();
        }
        String termino = consulta.trim();
        List<Publicacion> encontradas = publicacionRepository
                .findDistinctByArticuloNombreContainingIgnoreCaseOrArticuloDescripcionContainingIgnoreCaseOrObjetoACambiarContainingIgnoreCase(
                        termino, termino, termino);
        encontradas.forEach(this::prepararPublicacionParaLectura);
        return encontradas.stream()
                .filter(publicacion -> {
                    if (publicacion.getEstado() == null) {
                        publicacion.setEstado(EstadoPublicacion.ACTIVA);
                    }
                    return publicacion.estaActiva();
                })
                .sorted(Comparator.comparing(Publicacion::getFechaPublicacion, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
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
    public void eliminarPublicacion(Long publicacionId, String username, boolean esAdmin) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));

        if (!puedeGestionarPublicacion(publicacion, username, esAdmin)) {
            throw new AccessDeniedException("No tenés permiso para eliminar esta publicación");
        }
        ofertaRepository.deleteByPublicacionId(publicacionId);
        publicacionRepository.delete(publicacion);
    }

    @Transactional
    public Publicacion actualizarEstado(Long publicacionId, EstadoPublicacion nuevoEstado,
                                        String username, boolean esAdmin) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("Estado inválido");
        }
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));

        if (!puedeGestionarPublicacion(publicacion, username, esAdmin)) {
            throw new AccessDeniedException("No tenés permiso para actualizar esta publicación");
        }

        switch (nuevoEstado) {
            case ACTIVA -> publicacion.reactivar();
            case EN_NEGOCIACION -> publicacion.marcarEnNegociacion(LocalDateTime.now());
            case FINALIZADA -> publicacion.marcarFinalizada(LocalDateTime.now());
            case PAUSADA -> publicacion.pausar();
        }

        return publicacion;
    }

    @Transactional
    public Publicacion actualizarOficialidad(Long publicacionId, boolean oficial, String username, boolean esAdmin) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));

        if (!esAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden modificar la oficialidad");
        }

        publicacion.setOficial(oficial);
        return publicacion;
    }

    private List<ImagenEntrada> prepararEntradasImagen(CrearPublicacionDTO dto) {
        List<ImagenEntrada> entradas = new ArrayList<>();
        if (dto.getImagenesArchivos() != null) {
            for (MultipartFile archivo : dto.getImagenesArchivos()) {
                if (archivo == null || archivo.isEmpty()) {
                    continue;
                }
                try {
                    byte[] datos = archivo.getBytes();
                    if (datos.length == 0) {
                        continue;
                    }
                    entradas.add(new ImagenEntrada(datos, archivo.getContentType()));
                } catch (IOException e) {
                    throw new IllegalArgumentException("No se pudo leer la imagen: " + e.getMessage(), e);
                }
            }
        }
        if (dto.getImagenesBase64() != null) {
            for (int i = 0; i < dto.getImagenesBase64().size(); i++) {
                String base64Data = dto.getImagenesBase64().get(i);
                if (base64Data == null || base64Data.isBlank()) {
                    continue;
                }
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
                }
                byte[] datos = decodificarBase64(base64Data);
                if (datos.length == 0) {
                    continue;
                }
                String contentType = null;
                if (dto.getImagenesContentType() != null && dto.getImagenesContentType().size() > i) {
                    contentType = dto.getImagenesContentType().get(i);
                }
                entradas.add(new ImagenEntrada(datos, contentType));
            }
        }
        if (entradas.size() > 5) {
            return new ArrayList<>(entradas.subList(0, 5));
        }
        return entradas;
    }

    /**
     * Procesa y almacena la imagen asociada a una publicación aplicando
     * validaciones y optimizaciones.
     */
    private PublicacionImagen procesarImagenPublicacion(byte[] bytes, String contentType, int orden) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }

        String tipoNormalizado = normalizarContentType(contentType);
        byte[] originales = bytes;

        try {
            BufferedImage original = leerImagen(bytes);
            if (original == null) {
                log.warn("No se reconoce el formato de la imagen, se almacenará sin procesar");
                return crearImagenSinProcesar(originales, tipoNormalizado, orden);
            }

            ImagenProcesada procesada = optimizarImagen(bytes, tipoNormalizado, original);
            byte[] optimizadas = procesada.datos();
            if (optimizadas != null && optimizadas.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
            }
            PublicacionImagen imagen = new PublicacionImagen();
            imagen.setOrden(orden);
            imagen.setDatos(optimizadas);
            imagen.setContentType(procesada.contentType());
            return imagen;
        } catch (OutOfMemoryError e) {
            log.error("Sin memoria para procesar la imagen ({} bytes)", bytes != null ? bytes.length : -1, e);
            throw new IllegalArgumentException("La imagen es demasiado grande para procesarla. Reducila e intentá nuevamente.");
        } catch (IOException e) {
            log.warn("No se pudo procesar la imagen, se almacenará sin optimización", e);
            return crearImagenSinProcesar(originales, tipoNormalizado, orden);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo optimizar la imagen, se almacenará el archivo original", e);
            return crearImagenSinProcesar(originales, tipoNormalizado, orden);
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

        if (publicacion.getEstado() == null) {
            publicacion.setEstado(EstadoPublicacion.ACTIVA);
        }

        if (publicacion.getUsuario() != null) {
            publicacion.getUsuario().getUsername();
        }

        if (publicacion.getImagenesOrdenadas() != null) {
            for (PublicacionImagen imagen : publicacion.getImagenesOrdenadas()) {
                byte[] datos = imagen.getDatos();
                if (datos != null && datos.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(datos);
                    String contentType = imagen.getContentType();
                    if (contentType == null || contentType.isBlank()) {
                        contentType = "image/jpeg";
                    }
                    imagen.setBase64(base64);
                    imagen.setDataUri("data:" + contentType + ";base64," + base64);
                } else {
                    imagen.setBase64(null);
                    imagen.setDataUri(null);
                }
            }
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

        String normalizado = base64Data.replace(' ', '+');
        String limpio = BASE64_WHITESPACE.matcher(normalizado).replaceAll("");

        if (limpio.isEmpty()) {
            return new byte[0];
        }

        try {
            if (!BASE64_ALLOWED.matcher(limpio).matches()) {
                throw new IllegalArgumentException("Los datos de la imagen no están en formato Base64 válido");
            }
            return Base64.getMimeDecoder().decode(limpio);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Los datos de la imagen no están en formato Base64 válido", e);
        }
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
        if (contentType.equalsIgnoreCase("image/png")) {
            return "image/png";
        }
        if (contentType.equalsIgnoreCase("image/gif")) {
            return "image/gif";
        }
        if (contentType.equalsIgnoreCase("image/bmp")) {
            return "image/bmp";
        }
        if (contentType.toLowerCase().contains("jpeg")) {
            return "image/jpeg";
        }
        return contentType;
    }

    private boolean usuarioEsAdmin(ar.edu.huergo.swapify.entity.security.Usuario usuario) {
        return usuario.getRoles() != null && usuario.getRoles().stream()
                .anyMatch(rol -> rol.getNombre() != null && rol.getNombre().equalsIgnoreCase("ADMIN"));
    }

    private boolean puedeGestionarPublicacion(Publicacion publicacion, String username, boolean esAdmin) {
        if (publicacion == null) {
            return false;
        }
        if (esAdmin) {
            return true;
        }
        if (username == null || username.isBlank()) {
            return false;
        }
        return publicacion.getUsuario() != null
                && publicacion.getUsuario().getUsername() != null
                && publicacion.getUsuario().getUsername().equalsIgnoreCase(username.trim());
    }

    private ImagenProcesada optimizarImagen(byte[] bytes, String contentType, BufferedImage original) throws IOException {
        if (bytes == null || original == null) {
            throw new IllegalArgumentException("Imagen inválida");
        }

        boolean esJpeg = contentType != null && (contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/jpg"));

        EscaladoResult escalado = escalarSiEsNecesario(original, contentType);
        BufferedImage imagenBase = original;
        byte[] datos = bytes;
        if (escalado != null) {
            imagenBase = escalado.imagen();
            datos = escalado.datos();
        }

        if (!esJpeg && (contentType == null || contentType.isBlank() || !contentType.contains("png"))) {
            byte[] recomprimida = recomprimirComoJpeg(imagenBase);
            if (recomprimida != null && recomprimida.length < datos.length) {
                datos = recomprimida;
                contentType = "image/jpeg";
            }
        }

        if (datos != null && datos.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
        }

        return new ImagenProcesada(datos, contentType != null ? contentType : "image/jpeg");
    }

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

    private PublicacionImagen crearImagenSinProcesar(byte[] datosOriginales, String contentType, int orden) {
        if (datosOriginales == null || datosOriginales.length == 0) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
        if (datosOriginales.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB)");
        }
        PublicacionImagen imagen = new PublicacionImagen();
        imagen.setOrden(orden);
        imagen.setDatos(datosOriginales.clone());
        imagen.setContentType(contentType);
        return imagen;
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

    private record ImagenEntrada(byte[] datos, String contentType) {}

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
