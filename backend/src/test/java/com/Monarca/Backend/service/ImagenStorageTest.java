package com.Monarca.Backend.service;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
class ImagenStorageTest {
    @Test void enviaArchivoConClaveSoloAlServidorYDevuelveUrlPublica() throws Exception {
        var cliente=org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        var respuesta=org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(respuesta.statusCode()).thenReturn(200);
        org.mockito.Mockito.doReturn(respuesta).when(cliente).send(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        var salida=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",salida);
        var servicio=new ImagenStorageService("https://prueba.supabase.co","productos","clave-solo-test",cliente);
        var url=servicio.subir(new org.springframework.mock.web.MockMultipartFile("archivo","../foto.png","image/png",salida.toByteArray()),"blusa-123");
        assertTrue(url.startsWith("https://prueba.supabase.co/storage/v1/object/public/productos/catalogo/"));
        assertFalse(url.contains("clave-solo-test"));assertFalse(url.contains(".."));
        assertTrue(url.contains("/catalogo/blusa-123/"));
        assertThrows(IllegalArgumentException.class,()->servicio.subir(null,"../otro-producto"));
        var captura=org.mockito.ArgumentCaptor.forClass(java.net.http.HttpRequest.class);
        org.mockito.Mockito.verify(cliente).send(captura.capture(),org.mockito.ArgumentMatchers.any());
        assertEquals("POST",captura.getValue().method());assertEquals("Bearer clave-solo-test",captura.getValue().headers().firstValue("Authorization").orElseThrow());
        org.mockito.Mockito.when(respuesta.statusCode()).thenReturn(403);
        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->servicio.subir(new org.springframework.mock.web.MockMultipartFile("archivo",salida.toByteArray())));
        assertEquals(502,error.getStatusCode().value());assertFalse(error.getReason().contains("clave-solo-test"));
    }
    @Test void validaContenidoRealYRechazaArchivosDisfrazados() throws Exception {
        var salida=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",salida);
        assertEquals("png",ImagenStorageService.validar(salida.toByteArray()));
        assertThrows(IllegalArgumentException.class,()->ImagenStorageService.validar("<svg onload='alert(1)'/>".getBytes()));
    }
    @Test void sinClavesExplicaLaConfiguracionPendiente() {
        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new ImagenStorageService("","productos","").subir(null));
        assertEquals(503,error.getStatusCode().value());
    }
}
