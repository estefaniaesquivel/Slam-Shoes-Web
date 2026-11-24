package com.slamshoes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.slamshoes.exception.ValidacionException;

// REFACTOR: Extract Interface
// RAZON: Antes los servicios usaban directamente SupabaseStorageService con HttpClient incrustado; ahora existe IStorageService y esta clase
// prueba la implementacion con un HttpClient simulado, sin red (RF-10).
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes un archivo que no era WebP lanzaba IllegalArgumentException (el controlador la atrapaba y armaba el 400 a mano); ahora el servicio lanza ValidacionException, que GlobalExceptionHandler convierte en 400, y la prueba lo comprueba.
class SupabaseStorageServiceTest {

    private HttpClient httpClient;
    private SupabaseStorageService storage;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        storage = new SupabaseStorageService("https://demo.supabase.co", "slam-bucket", "secret-key", httpClient);
    }

    @Test
    void estaConfiguradoValidaLlave() {
        assertThat(storage.estaConfigurado()).isTrue();
        SupabaseStorageService sinLlave = new SupabaseStorageService("https://demo.supabase.co", "slam-bucket", "  ", httpClient);
        assertThat(sinLlave.estaConfigurado()).isFalse();
    }

    @Test
    void utilidadesSlugUrlPublicaYRutaDesdeUrl() {
        assertThat(SupabaseStorageService.slug("Nike Kyrie 8 N7!")).isEqualTo("nike-kyrie-8-n7");
        assertThat(SupabaseStorageService.slug("   ")).isEqualTo("imagen");
        String url = storage.urlPublica("banners/hero.webp");
        assertThat(url).isEqualTo("https://demo.supabase.co/storage/v1/object/public/slam-bucket/banners/hero.webp");
        assertThat(storage.rutaDesdeUrl(url)).isEqualTo("banners/hero.webp");
        assertThat(storage.rutaDesdeUrl("https://otro.sitio/img.webp")).isNull();
    }

    @Test
    void esWebpValidaExtensionYContentType() {
        MockMultipartFile valido = new MockMultipartFile("img", "foto.webp", "image/webp", new byte[]{1, 2});
        MockMultipartFile porExtension = new MockMultipartFile("img", "foto.WEBP", "application/octet-stream", new byte[]{1});
        MockMultipartFile invalido = new MockMultipartFile("img", "foto.png", "image/png", new byte[]{1});
        MockMultipartFile vacio = new MockMultipartFile("img", "foto.webp", "image/webp", new byte[0]);

        assertThat(SupabaseStorageService.esWebp(valido)).isTrue();
        assertThat(SupabaseStorageService.esWebp(porExtension)).isTrue();
        assertThat(SupabaseStorageService.esWebp(invalido)).isFalse();
        assertThat(SupabaseStorageService.esWebp(vacio)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void subirWebpRetornaUrlPublicaCuandoHttpResponde200() throws Exception {
        HttpResponse<Object> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(resp);

        MockMultipartFile archivo = new MockMultipartFile("imagen", "tenis.webp", "image/webp", new byte[]{10, 20});
        String url = storage.subirWebp("productos", "Air Jordan", archivo);

        assertThat(url).startsWith("https://demo.supabase.co/storage/v1/object/public/slam-bucket/productos/air-jordan-");
        assertThat(url).endsWith(".webp");
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Antes IllegalArgumentException atrapada en el controlador; ahora ValidacionException lanzada por subirWebp.
    @Test
    void subirWebpRechazaArchivoNoWebp() {
        MockMultipartFile archivo = new MockMultipartFile("imagen", "tenis.jpg", "image/jpeg", new byte[]{10});
        assertThatThrownBy(() -> storage.subirWebp("productos", "Air Jordan", archivo))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void eliminarNoPropagaExcepcionSiFallaRed() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("timeout"));
        storage.eliminar(List.of("productos/1.webp"));
        verify(httpClient).send(any(HttpRequest.class), any());
    }
}
