package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.PasswordResetToken;
import com.prestarte.tfg.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalida cualquier token previo del mismo usuario al generar uno nuevo.
     * Evita que queden tokens válidos colgando si el usuario solicita reseteo
     * varias veces.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(@Param("user") User user);
}
