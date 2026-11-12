package com.slamshoes.dto;

// En la respuesta: url + orden guardados. En la edición: url = imagen existente que se conserva, null = imagen nueva (archivo).
public record ImagenAdminDTO(String url, Integer orden) {}
