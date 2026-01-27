package ar.edu.huergo.swapify.service.producto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ar.edu.huergo.swapify.dto.producto.ProductoRequestDTO;
import ar.edu.huergo.swapify.dto.producto.ProductoResponseDTO;
import ar.edu.huergo.swapify.entity.producto.Producto;
import ar.edu.huergo.swapify.mapper.producto.ProductoMapper;
import ar.edu.huergo.swapify.repository.producto.ProductoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Autowired
    public ProductoService(ProductoRepository productoRepository, ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    public ProductoResponseDTO createProducto(ProductoRequestDTO requestDTO) {
        Producto producto = productoMapper.toEntity(requestDTO);
        producto = productoRepository.save(producto);
        return productoMapper.toDTO(producto);
    }

    public ProductoResponseDTO getProductoById(Long id) {
        Producto producto = productoRepository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return productoMapper.toDTO(producto);
    }

    public List<ProductoResponseDTO> getAllProductos() {
        List<Producto> productos = productoRepository.findAll();
        return productos.stream().map(productoMapper::toDTO).collect(Collectors.toList());
    }

    public ProductoResponseDTO updateProducto(Long id, ProductoRequestDTO requestDTO) {
        Producto producto = productoRepository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setNombre(requestDTO.getNombre());
        producto.setCategoria(requestDTO.getCategoria());
        producto.setPrecio(requestDTO.getPrecio());
        producto = productoRepository.save(producto);
        return productoMapper.toDTO(producto);
    }

    public void deleteProducto(Long id) {
        productoRepository.deleteById(id);
    }
}
