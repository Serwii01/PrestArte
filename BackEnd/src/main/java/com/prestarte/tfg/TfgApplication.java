package com.prestarte.tfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Prestarte.
 *
 * Spring Boot detecta automáticamente los componentes (controladores,
 * servicios, repositorios y configuraciones) que componen la API REST
 * y los arranca en un contexto único.
 */
@SpringBootApplication
public class TfgApplication {

    public static void main(String[] args) {
        SpringApplication.run(TfgApplication.class, args);
    }

}
