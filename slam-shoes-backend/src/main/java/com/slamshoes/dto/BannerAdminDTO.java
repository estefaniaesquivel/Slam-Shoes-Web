package com.slamshoes.dto;

// Datos del formulario de banners (la imagen llega aparte como archivo)
public record BannerAdminDTO(String titulo, String enlaceDestino, Integer orden, Boolean activo) {}
