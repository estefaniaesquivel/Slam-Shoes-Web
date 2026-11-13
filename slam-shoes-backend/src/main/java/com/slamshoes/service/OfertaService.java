package com.slamshoes.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.slamshoes.model.Oferta;
import com.slamshoes.repository.OfertaRepository;

@Service
public class OfertaService {

    @Autowired
    private OfertaRepository ofertaRepository;

    // Ofertas activas cuyo periodo incluye el momento actual
    public List<Oferta> vigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        return ofertaRepository.findByActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(ahora, ahora);
    }

    // Todas las ofertas activas que aún no terminaron: las vigentes hoy y las programadas a futuro.
    // Se usan para LISTAR y mostrar el descuento; el cobro sigue usando solo las vigentes.
    public List<Oferta> visibles() {
        return ofertaRepository.findByActivaTrueAndFechaFinGreaterThanEqual(LocalDateTime.now());
    }

    public boolean esVigente(Oferta o) {
        LocalDateTime ahora = LocalDateTime.now();
        return !o.getFechaInicio().isAfter(ahora) && !o.getFechaFin().isBefore(ahora);
    }

    // Mejor oferta visible por producto: si tiene una vigente gana esa; si no, la futura de mayor descuento
    public Map<Long, Oferta> mejorVisiblePorProducto() {
        List<Oferta> todas = visibles();
        Map<Long, Oferta> mejores = new HashMap<>(mejorPorProducto(todas.stream().filter(this::esVigente).toList()));
        mejorPorProducto(todas.stream().filter(o -> !esVigente(o)).toList())
                .forEach(mejores::putIfAbsent);
        return mejores;
    }

    // Si un producto tiene varias ofertas vigentes se queda con la de mayor descuento
    public Map<Long, Oferta> mejorPorProducto(List<Oferta> ofertas) {
        Map<Long, Oferta> mejores = new HashMap<>();
        for (Oferta o : ofertas) {
            Oferta actual = mejores.get(o.getIdProducto());
            if (actual == null || o.getPorcentajeDescuento() > actual.getPorcentajeDescuento()) {
                mejores.put(o.getIdProducto(), o);
            }
        }
        return mejores;
    }

    public Optional<Oferta> mejorDeProducto(Long idProducto) {
        return Optional.ofNullable(mejorPorProducto(vigentes()).get(idProducto));
    }

    // precio * (1 - porcentaje/100), redondeado a 2 decimales
    public double precioFinal(double precio, Oferta oferta) {
        if (oferta == null) return Math.round(precio * 100.0) / 100.0;
        double final_ = precio * (1 - oferta.getPorcentajeDescuento() / 100.0);
        return Math.round(final_ * 100.0) / 100.0;
    }
}
