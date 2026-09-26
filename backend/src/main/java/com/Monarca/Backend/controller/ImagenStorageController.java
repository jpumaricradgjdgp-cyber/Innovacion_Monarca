package com.Monarca.Backend.controller;

import com.Monarca.Backend.service.ImagenStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/imagenes")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ImagenStorageController {
    private final ImagenStorageService storage;
    public ImagenStorageController(ImagenStorageService storage) { this.storage=storage; }
    @PostMapping(consumes="multipart/form-data")
    public Map<String,String> subir(@RequestParam("archivo") MultipartFile archivo,
            @RequestParam(defaultValue="sin-clasificar") String carpeta) {
        return Map.of("url",storage.subir(archivo,carpeta));
    }
}
