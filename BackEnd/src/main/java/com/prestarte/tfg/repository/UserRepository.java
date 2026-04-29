package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.model.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByStatus(UserStatus status);
}