package com.Monarca.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class ImagenStorageService {
    private final String url, bucket, clave;
    private final HttpClient cliente;
    @org.springframework.beans.factory.annotation.Autowired
    public ImagenStorageService(@Value("${monarca.storage.url:}") String url,
            @Value("${monarca.storage.bucket:productos}") String bucket,
            @Value("${monarca.storage.key:}") String clave) {
        this(url,bucket,clave,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }
    ImagenStorageService(String url, String bucket, String clave, HttpClient cliente) {
        this.url=url.replaceAll("/+$", ""); this.bucket=bucket; this.clave=clave;
        this.cliente=cliente;
    }
    public String subir(MultipartFile archivo) {
        return subir(archivo,"sin-clasificar");
    }
    public String subir(MultipartFile archivo, String carpeta) {
        if (carpeta==null || !carpeta.matches("[a-z0-9][a-z0-9-]{0,99}"))
            throw new IllegalArgumentException("La carpeta del producto no es válida.");
        if (url.isBlank() || clave.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Falta configurar la carga de imágenes. Ejecuta Configurar-Imagenes.ps1 y reinicia el servidor.");
        if (!url.matches("https://[a-z0-9-]+\\.supabase\\.co") || !bucket.matches("[a-zA-Z0-9_-]{1,100}"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Revisa la URL y el bucket de imágenes configurados.");
        byte[] datos;
        String formato;
        try {
            if (archivo==null || archivo.isEmpty() || archivo.getSize()>5*1024*1024)
                throw new IllegalArgumentException("Selecciona una imagen JPG o PNG de hasta 5 MB.");
            datos=archivo.getBytes();
            formato=validar(datos);
        } catch (IOException e) { throw new IllegalArgumentException("No se pudo leer la imagen."); }
        String ruta="catalogo/"+carpeta+"/"+UUID.randomUUID()+"."+formato;
        var request=HttpRequest.newBuilder(URI.create(url+"/storage/v1/object/"+bucket+"/"+ruta))
                .timeout(Duration.ofSeconds(40)).header("apikey",clave).header("Authorization","Bearer "+clave)
                .header("Content-Type",formato.equals("png")?"image/png":"image/jpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(datos)).build();
        try {
            var respuesta=cliente.send(request,HttpResponse.BodyHandlers.discarding());
            if (respuesta.statusCode()<200 || respuesta.statusCode()>=300)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Supabase rechazó la imagen. Revisa la clave de servidor y el bucket público.");
            return url+"/storage/v1/object/public/"+bucket+"/"+ruta;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La carga fue interrumpida; vuelve a intentarlo.");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No se pudo conectar con Supabase para subir la imagen.");
        }
    }
    static String validar(byte[] datos) throws IOException {
        try (var entrada=ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {
            var lectores=ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new IllegalArgumentException("El archivo no es una imagen JPG o PNG válida.");
            var lector=lectores.next();
            try {
                lector.setInput(entrada); String formato=lector.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png")) throw new IllegalArgumentException("Usa imágenes JPG o PNG.");
                if ((long)lector.getWidth(0)*lector.getHeight(0)>40000000)
                    throw new IllegalArgumentException("La imagen es demasiado grande. Usa una de hasta 40 megapíxeles.");
                lector.read(0);
                return formato.equals("jpeg")?"jpg":"png";
            } finally { lector.dispose(); }
        }
    }
}
