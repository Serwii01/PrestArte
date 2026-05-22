package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.PasswordResetToken;
import com.prestarte.tfg.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a la tabla de tokens de recuperación de contraseña.
 *
 * Además de la búsqueda por el propio token, ofrece una operación de
 * invalidación masiva por usuario, utilizada para anular cualquier
 * token anterior cuando se emite uno nuevo.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Busca un token a partir de su valor textual. */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Marca como utilizados todos los tokens vigentes del usuario
     * indicado, de manera que solo el último emitido siga siendo válido.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(@Param("user") User user);
}
