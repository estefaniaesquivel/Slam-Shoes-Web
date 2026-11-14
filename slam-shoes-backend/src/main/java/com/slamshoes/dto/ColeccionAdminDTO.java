package com.slamshoes.dto;

import java.util.List;

public record ColeccionAdminDTO(String nombre, String descripcion, List<Long> idsProductos) {}
