package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.model.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Listado de pendientes de aprobación. Cargamos el documento de
     * verificación en el mismo query (EntityGraph) para que el admin
     * lo pueda ver sin lazy-init issues fuera de la transacción.
     */
    @EntityGraph(attributePaths = "verificationFile")
    List<User> findByStatus(UserStatus status);

    @EntityGraph(attributePaths = "verificationFile")
    List<User> findByRole(Role role);

    @EntityGraph(attributePaths = "verificationFile")
    @Override
    List<User> findAll();

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
