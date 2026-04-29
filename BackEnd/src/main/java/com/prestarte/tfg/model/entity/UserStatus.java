package com.prestarte.tfg.model.entity;

public enum UserStatus {
    PENDING,    // Recién registrado, esperando al admin
    APPROVED,   // Verificado y puede loguear
    REJECTED    // El admin ha denegado el acceso
}