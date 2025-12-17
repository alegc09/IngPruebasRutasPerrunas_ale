package com.tzilacatzin.rutasperrunas.model

data class Paseo(
    var id: String = "",            // ID del documento
    val idDuenio: String = "",      // UID del dueño
    val idPaseador: String = "",    // UID del paseador (vacío al inicio)
    val nombresMascotas: List<String> = emptyList(), // Lista de nombres de perros
    val estado: String = "SOLICITADO", // SOLICITADO, ACEPTADO, EN_PASEO, FINALIZADO
    val codigoFin: String = "",     // Código secreto
    val costoTotal: Double = 0.0,
    val latitud: Double = 0.0,      // Ubicación inicial
    val longitud: Double = 0.0
)