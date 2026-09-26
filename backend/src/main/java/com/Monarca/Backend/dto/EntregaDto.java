package com.Monarca.Backend.dto;
public record EntregaDto(String nombre, String apellidos, String correo, String telefono,
                         String dni, String direccion, String distrito, String referencia) {
    public String validarYSerializar() {
        campo(nombre, "Nombres", 60); campo(apellidos, "Apellidos", 60);
        campo(direccion, "Dirección", 150); campo(distrito, "Distrito", 50);
        if (!com.Monarca.Backend.service.PasswordRecoveryService.emailValido(correo)) throw new IllegalArgumentException("Correo de entrega inválido");
        if (telefono == null || !telefono.matches("[+0-9 ()-]{7,20}")) throw new IllegalArgumentException("Teléfono inválido");
        if (dni == null || !dni.matches("[A-Za-z0-9-]{6,20}")) throw new IllegalArgumentException("Documento inválido");
        if (referencia != null && (referencia.length() > 100 || referencia.contains("\n") || referencia.contains("\r"))) throw new IllegalArgumentException("Referencia inválida");
        String texto = "Nombre: " + nombre.trim() + " " + apellidos.trim() + "\nCorreo: " + correo.trim()
                + "\nTeléfono: " + telefono.trim() + "\nDocumento: " + dni.trim() + "\nDirección: " + direccion.trim()
                + "\nDistrito: " + distrito.trim() + "\nReferencia: " + (referencia == null ? "" : referencia.trim());
        if (texto.length() > 500) throw new IllegalArgumentException("Reduce los datos de entrega a 500 caracteres en total");
        return texto;
    }
    private static void campo(String valor, String nombre, int limite) {
        if (valor == null || valor.isBlank() || valor.length() > limite || valor.contains("\n") || valor.contains("\r"))
            throw new IllegalArgumentException(nombre + " requerido, máximo " + limite + " caracteres");
    }
}
