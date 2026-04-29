package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.DBFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DBFileRepository extends JpaRepository<DBFile, String> {
    // Usamos String porque en tu entidad DBFile el ID es un UUID (String)
}