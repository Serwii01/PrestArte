package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.model.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla de usuarios.
 *
 * Ofrece las consultas habituales por estado, por rol y por email, además
 * de las comprobaciones de unicidad utilizadas en el registro. En las
 * consultas de listado se utiliza {@code @EntityGraph} para traer en el
 * mismo query el documento de verificación, de modo que el panel del
 * administrador pueda mostrarlo sin necesidad de mantener abierta la
 * sesión de persistencia.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Devuelve los usuarios con el estado indicado (típicamente PENDING). */
    @EntityGraph(attributePaths = "verificationFile")
    List<User> findByStatus(UserStatus status);

    /** Devuelve los usuarios cuyo rol coincide con el indicado. */
    @EntityGraph(attributePaths = "verificationFile")
    List<User> findByRole(Role role);

    /** Devuelve la lista completa de usuarios. */
    @EntityGraph(attributePaths = "verificationFile")
    @Override
    List<User> findAll();

    /** Busca un usuario por su correo electrónico. */
    Optional<User> findByEmail(String email);

    /** Comprueba si ya existe una cuenta con ese email. */
    boolean existsByEmail(String email);

    /** Comprueba si ya existe una cuenta con ese teléfono. */
    boolean existsByPhone(String phone);

    /** Comprueba si ya existe una cuenta con ese DNI / NIE / CIF. */
    boolean existsByTaxId(String taxId);
}
