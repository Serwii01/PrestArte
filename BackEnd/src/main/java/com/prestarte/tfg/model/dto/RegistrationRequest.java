package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload del formulario de registro.
 *
 * Todos los campos son obligatorios. El documento de verificación
 * viaja en una parte aparte de la petición multipart y se valida
 * en {@code UserService}. Las anotaciones definen un primer nivel de
 * comprobación de formato; la validación final de la letra o el
 * dígito de control del identificador fiscal se aplica también en el
 * servicio.
 */
@Data
public class RegistrationRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^(?:\\+?34)?[6-9]\\d{8}$",
            message = "Teléfono no válido. 9 dígitos empezando por 6, 7, 8 o 9 (con o sin +34).")
    private String phone;

    /** Identificador fiscal: admite DNI, NIE o CIF en sus formatos habituales. */
    @NotBlank(message = "El DNI / NIE / CIF es obligatorio")
    @Pattern(regexp = "^(\\d{8}[A-Za-z]|[XYZxyz]\\d{7}[A-Za-z]|[ABCDEFGHJNPQRSUVWabcdefghjnpqrsuvw]\\d{7}[0-9A-Ja-j])$",
            message = "Formato de DNI / NIE / CIF no válido")
    private String taxId;

    /**
     * Rol elegido en el formulario. Solo se aceptan los roles que se
     * pueden crear desde el registro público; el rol de administrador
     * se crea exclusivamente en la rutina de arranque.
     */
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "COLLECTOR|FOUNDATION|MUSEUM|TRANSPORT",
            message = "El rol debe ser COLLECTOR, FOUNDATION o TRANSPORT")
    private String role;
}
