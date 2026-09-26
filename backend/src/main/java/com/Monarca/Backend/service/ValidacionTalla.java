package com.Monarca.Backend.service;

final class ValidacionTalla {
    private ValidacionTalla() {}
    static void comprobar(String talla) {
        if (talla == null) return;
        String valor=talla.trim();
        if (valor.matches(".*[-–—,;/+|].*") || valor.matches("(?i)(?:XXS|XS|S|M|L|XL|XXL|XXXL|[0-9]+)(?:\\s+(?:XXS|XS|S|M|L|XL|XXL|XXXL|[0-9]+))+"))
            throw new IllegalArgumentException("Escribe una sola talla, por ejemplo S. Añade M y L como variantes separadas, cada una con sus propias unidades.");
    }
}
