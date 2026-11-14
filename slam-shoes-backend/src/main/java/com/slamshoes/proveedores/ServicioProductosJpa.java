package com.slamshoes.proveedores;

import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerLong;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.model.Categoria;
import com.slamshoes.model.Oferta;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoImagen;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.CategoriaRepository;
import com.slamshoes.repository.ProductoImagenRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;
import com.slamshoes.service.OfertaService;

/**
 * Proveedor REAL del servicio "productos" (con base de datos): catalogo, detalle y existencias.
 *
 * Toda la logica de "como se guarda un producto, sus tallas, imagenes, categoria y oferta" vive
 * AQUI y no en el frontend ni en los controladores. Si manana cambia la estructura de las tallas,
 * solo cambia este proveedor: el cliente (Vue) sigue pidiendo "productos.obtenerDetalle".
 *
 * Operaciones:
 *  - listarProductos {}                       -> productos activos (resumen)
 *  - obtenerDetalle  {idProducto}             -> producto + categoria + oferta + tallas + imagenes + variantes de modelo
 *  - consultarStock  {idVariante, cantidad?}  -> existencias de una talla (y si alcanzan para "cantidad")
 */
@Component
public class ServicioProductosJpa implements ProveedorServicio {

    private static final String NOMBRE_CLASE = "ServicioProductosJpa";
    private static final Set<String> OPERACIONES = Set.of("listarProductos", "obtenerDetalle", "consultarStock");

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private OfertaService ofertaService;

    @Override
    public String getNombreServicio() {
        return "productos";
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return OPERACIONES;
    }

    @Override
    public Respuesta ejecutar(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        try {
            switch (solicitud.getOperacion()) {
                case "listarProductos":
                    return Respuesta.exito(id, listarProductos());
                case "obtenerDetalle":
                    return Respuesta.exito(id, obtenerDetalle(solicitud.getParametros()));
                case "consultarStock":
                    return Respuesta.exito(id, consultarStock(solicitud.getParametros()));
                default:
                    return Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                            "Operacion no soportada: " + solicitud.getOperacion());
            }
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }

    private Map<String, Object> listarProductos() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Producto p : productoRepository.findAll()) {
            if (Boolean.FALSE.equals(p.getActivo())) {
                continue;
            }
            lista.add(mapa("idProducto", p.getIdProducto(), "nombre", p.getNombre(), "marca", p.getMarca(),
                    "precio", p.getPrecio(), "imagenUrl", p.getImagenUrl(), "idCategoria", p.getIdCategoria()));
        }
        return mapa("atendidoPor", NOMBRE_CLASE, "totalProductos", lista.size(), "productos", lista);
    }

    private Map<String, Object> obtenerDetalle(Map<String, Object> parametros) {
        long idProducto = obtenerLong(parametros, "idProducto");
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("No existe un producto con idProducto=" + idProducto));

        // Categoria
        Map<String, Object> categoria = null;
        if (p.getIdCategoria() != null) {
            Categoria c = categoriaRepository.findById(p.getIdCategoria()).orElse(null);
            if (c != null) {
                categoria = mapa("idCategoria", c.getIdCategoria(), "nombre", c.getNombre(),
                        "descripcion", c.getDescripcion());
            }
        }

        // Oferta (vigente o programada) y precio final de hoy
        Oferta oferta = ofertaService.mejorVisiblePorProducto().get(idProducto);
        double precioBase = p.getPrecio() == null ? 0 : p.getPrecio();
        Map<String, Object> datosOferta = null;
        double precioFinal = ofertaService.precioFinal(precioBase, null);
        if (oferta != null) {
            boolean vigente = ofertaService.esVigente(oferta);
            double conDescuento = ofertaService.precioFinal(precioBase, oferta);
            datosOferta = mapa("idOferta", oferta.getIdOferta(), "nombre", oferta.getNombre(),
                    "porcentajeDescuento", oferta.getPorcentajeDescuento(),
                    "fechaInicio", oferta.getFechaInicio(), "fechaFin", oferta.getFechaFin(),
                    "vigente", vigente, "precioConDescuento", conDescuento);
            if (vigente) {
                precioFinal = conDescuento;
            }
        }

        // Tallas con existencias
        List<Map<String, Object>> tallas = new ArrayList<>();
        List<ProductoTalla> variantesTalla = new ArrayList<>(productoTallaRepository.findByIdProducto(idProducto));
        variantesTalla.sort(Comparator.comparing(ProductoTalla::getIdVariante));
        for (ProductoTalla t : variantesTalla) {
            tallas.add(mapa("idVariante", t.getIdVariante(), "idProducto", t.getIdProducto(),
                    "talla", t.getTalla(), "color", t.getColor(), "stock", t.getStock() == null ? 0 : t.getStock()));
        }

        // Galeria de imagenes ordenada
        List<ProductoImagen> imagenesBD = new ArrayList<>(productoImagenRepository.findByIdProducto(idProducto));
        imagenesBD.sort(Comparator.comparing(ProductoImagen::getOrden, Comparator.nullsLast(Comparator.naturalOrder())));
        List<Map<String, Object>> imagenes = new ArrayList<>();
        for (ProductoImagen im : imagenesBD) {
            imagenes.add(mapa("urlImagen", im.getUrlImagen(), "orden", im.getOrden()));
        }

        // Otros tenis del mismo modelo (columna productos.modelo)
        String modelo = p.getModelo() == null ? "" : p.getModelo().trim();
        List<Producto> mismos = modelo.isEmpty() ? List.of(p) : productoRepository.findByModeloIgnoreCase(modelo);
        List<Map<String, Object>> variantes = new ArrayList<>();
        mismos.stream()
                .filter(m -> m.getIdProducto().equals(idProducto) || !Boolean.FALSE.equals(m.getActivo()))
                .sorted(Comparator.comparing(Producto::getIdProducto))
                .forEach(m -> variantes.add(mapa("idProducto", m.getIdProducto(), "nombre", m.getNombre(),
                        "imagenUrl", m.getImagenUrl())));

        Map<String, Object> producto = mapa("idProducto", p.getIdProducto(), "nombre", p.getNombre(),
                "marca", p.getMarca(), "descripcion", p.getDescripcion(), "precio", p.getPrecio(),
                "imagenUrl", p.getImagenUrl(), "idCategoria", p.getIdCategoria(), "idMarca", p.getIdMarca(),
                "idJugador", p.getIdJugador(), "idColeccion", p.getIdColeccion(), "modelo", p.getModelo(),
                "activo", p.getActivo(), "esTendencia", p.getEsTendencia());

        return mapa("atendidoPor", NOMBRE_CLASE, "producto", producto, "categoria", categoria,
                "precioFinal", precioFinal, "oferta", datosOferta, "tallas", tallas,
                "imagenes", imagenes, "variantes", variantes);
    }

    private Map<String, Object> consultarStock(Map<String, Object> parametros) {
        long idVariante = obtenerLong(parametros, "idVariante");
        ProductoTalla v = productoTallaRepository.findById(idVariante)
                .orElseThrow(() -> new IllegalArgumentException("No existe una talla con idVariante=" + idVariante));
        int stock = v.getStock() == null ? 0 : v.getStock();

        Map<String, Object> resultado = mapa("atendidoPor", NOMBRE_CLASE, "idVariante", v.getIdVariante(),
                "idProducto", v.getIdProducto(), "talla", v.getTalla(), "color", v.getColor(),
                "stock", stock, "disponible", stock > 0);
        if (parametros.get("cantidad") != null) {
            long cantidad = obtenerLong(parametros, "cantidad");
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero, se recibio " + cantidad);
            }
            resultado.put("cantidadSolicitada", cantidad);
            resultado.put("suficiente", stock >= cantidad);
        }
        return resultado;
    }
}
