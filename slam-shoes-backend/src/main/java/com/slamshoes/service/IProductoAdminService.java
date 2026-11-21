package com.slamshoes.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;

public interface IProductoAdminService {
    ProductoAdminDetalleDTO obtenerDetalle(Long id);
    Map<String, Object> crearCompleto(ProductoNuevoDTO datos, List<MultipartFile> imagenes);
    Map<String, Object> editarCompleto(Long id, ProductoEdicionDTO datos, List<MultipartFile> nuevas);
    void eliminar(Long idProducto);
}
