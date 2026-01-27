package ar.edu.huergo.swapify.mapper.producto;

import org.springframework.stereotype.Component;
import ar.edu.huergo.swapify.dto.producto.ProductoRequestDTO;
import ar.edu.huergo.swapify.dto.producto.ProductoResponseDTO;
import ar.edu.huergo.swapify.entity.producto.Producto;

@Component
public class ProductoMapper {

    public Producto toEntity(ProductoRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setCategoria(dto.getCategoria());
        producto.setPrecio(dto.getPrecio());
        return producto;
    }

    public ProductoResponseDTO toDTO(Producto producto) {
        if (producto == null) {
            return null;
        }
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setCategoria(producto.getCategoria());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        return dto;
    }
}