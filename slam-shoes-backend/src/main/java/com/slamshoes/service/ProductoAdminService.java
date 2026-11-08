package com.slamshoes.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoImagen;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.ProductoImagenRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;

@Service
public class ProductoAdminService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;

    // Crea el producto, sus variantes (producto_tallas) y sus imágenes (producto_imagenes) en una sola transacción.
    // Si algo falla, no se guarda nada.
    @Transactional
    public Producto guardar(ProductoNuevoDTO d, String nombreMarca, Long idColeccion, Long idJugador, List<String> urlsImagenes) {
        Producto p = new Producto();
        p.setNombre(d.nombre().trim());
        p.setDescripcion(d.descripcion() == null || d.descripcion().isBlank() ? null : d.descripcion().trim());
        p.setPrecio(d.precio());
        p.setIdCategoria(d.idCategoria());
        p.setIdMarca(d.idMarca());
        p.setMarca(nombreMarca);
        p.setIdColeccion(idColeccion);
        p.setIdJugador(idJugador);
        p.setActivo(d.activo() == null ? Boolean.TRUE : d.activo());
        p.setModelo(d.modelo() == null || d.modelo().isBlank() ? null : d.modelo().trim());
        p.setEsTendencia(Boolean.TRUE.equals(d.esTendencia()));
        p.setImagenUrl(urlsImagenes.get(0)); // la imagen de orden 1 es la portada
        p = productoRepository.save(p); // aquí se genera el id_producto

        for (ColorTallasDTO c : d.colores()) {
            for (TallaStockDTO t : c.tallas()) {
                ProductoTalla v = new ProductoTalla();
                v.setIdProducto(p.getIdProducto());
                v.setColor(c.color());
                v.setTalla(t.talla());
                v.setStock(t.stock() == null ? 0 : t.stock());
                productoTallaRepository.save(v);
            }
        }

        for (int i = 0; i < urlsImagenes.size(); i++) {
            ProductoImagen img = new ProductoImagen();
            img.setIdProducto(p.getIdProducto());
            img.setUrlImagen(urlsImagenes.get(i));
            img.setOrden(i + 1);
            productoImagenRepository.save(img);
        }
        return p;
    }
}
