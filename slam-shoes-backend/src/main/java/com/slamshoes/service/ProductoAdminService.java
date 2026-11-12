package com.slamshoes.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoImagen;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.PedidoDetalleRepository;
import com.slamshoes.repository.ProductoImagenRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;

@Service
public class ProductoAdminService {

    public static final String SIN_COLOR = "Sin color";

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepository;

    private static String colorBD(String color) {
        return SIN_COLOR.equals(color) ? null : color;
    }

    private static String clave(String color, String talla) {
        return (color == null ? SIN_COLOR : color).toLowerCase() + "|" + talla;
    }

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
                v.setColor(colorBD(c.color()));
                v.setTalla(t.talla());
                v.setStock(t.stock() == null ? 0 : t.stock());
                productoTallaRepository.save(v);
            }
        }

        guardarImagenes(p.getIdProducto(), urlsImagenes);
        return p;
    }

    // Actualiza el producto, sincroniza sus variantes y reemplaza su galería (todo o nada)
    @Transactional
    public Producto actualizar(Long idProducto, ProductoEdicionDTO d, String nombreMarca, Long idColeccion,
                               Long idJugador, List<String> urlsImagenes) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe"));

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
        p.setImagenUrl(urlsImagenes.get(0));
        p = productoRepository.save(p);

        sincronizarVariantes(idProducto, d.colores());

        productoImagenRepository.deleteByIdProducto(idProducto);
        guardarImagenes(idProducto, urlsImagenes);
        return p;
    }

    private void guardarImagenes(Long idProducto, List<String> urls) {
        for (int i = 0; i < urls.size(); i++) {
            ProductoImagen img = new ProductoImagen();
            img.setIdProducto(idProducto);
            img.setUrlImagen(urls.get(i));
            img.setOrden(i + 1); // el orden es la posición en el formulario
            productoImagenRepository.save(img);
        }
    }

    // Variantes (color + talla): se actualiza el stock de las existentes, se crean las nuevas y se quitan las que ya no están.
    // Una variante que ya aparece en algún pedido no se puede borrar (historial): se deja con stock 0.
    private void sincronizarVariantes(Long idProducto, List<ColorTallasDTO> colores) {
        Map<String, ProductoTalla> actuales = new HashMap<>();
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(idProducto)) {
            actuales.put(clave(v.getColor(), v.getTalla()), v);
        }

        Set<String> deseadas = new HashSet<>();
        for (ColorTallasDTO c : colores) {
            for (TallaStockDTO t : c.tallas()) {
                String k = clave(colorBD(c.color()), t.talla());
                deseadas.add(k);
                ProductoTalla v = actuales.get(k);
                if (v == null) {
                    v = new ProductoTalla();
                    v.setIdProducto(idProducto);
                    v.setColor(colorBD(c.color()));
                    v.setTalla(t.talla());
                }
                v.setStock(t.stock() == null ? 0 : t.stock());
                productoTallaRepository.save(v);
            }
        }

        for (Map.Entry<String, ProductoTalla> e : actuales.entrySet()) {
            if (deseadas.contains(e.getKey())) continue;
            ProductoTalla v = e.getValue();
            if (pedidoDetalleRepository.existsByIdVariante(v.getIdVariante())) {
                v.setStock(0);
                productoTallaRepository.save(v);
            } else {
                productoTallaRepository.delete(v); // sus filas de carrito_detalle se borran en cascada
            }
        }
    }

    // Elimina el producto. Sus variantes, imágenes, favoritos, ofertas y novedades se borran en cascada (FK).
    // Si ya fue comprado no se puede borrar sin perder el historial de pedidos.
    @Transactional
    public void eliminar(Long idProducto) {
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(idProducto)) {
            if (pedidoDetalleRepository.existsByIdVariante(v.getIdVariante())) {
                throw new IllegalStateException(
                        "Este producto ya aparece en pedidos y no se puede eliminar. Márcalo como inactivo desde su edición.");
            }
        }
        productoRepository.deleteById(idProducto);
    }
}
