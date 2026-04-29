package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.DBFileRepository;
import com.prestarte.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DBFileRepository dbFileRepository;

    @Transactional
    public User registerUser(RegistrationRequest request, MultipartFile file) throws IOException {
        // 1. Guardar el documento de identidad (DNI/CIF) en la tabla db_files
        DBFile verificationDoc = DBFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .build();
        dbFileRepository.save(verificationDoc);

        // 2. DECIDIR QUÉ TIPO DE USUARIO CREAR E INSTANCIAR LA SUBCLASE CORRECTA
        User user;
        String roleStr = request.getRole() != null ? request.getRole().toUpperCase() : "COLLECTOR";

        switch (roleStr) {
            case "MUSEUM":
            case "FOUNDATION":
                Foundation foundation = new Foundation();
                // REGLA CLAVE: Rellenar el campo obligatorio que dio el error 500
                foundation.setInstitutionName(request.getName());
                // Puedes poner valores por defecto si tu BD pide address/city y no vienen en el DTO
                foundation.setAddress("Pendiente de completar");
                foundation.setCity("Pendiente de completar");

                user = foundation;
                user.setRole(Role.FOUNDATION);
                break;

            case "TRANSPORT":
                TransportCompany transport = new TransportCompany();
                // Si TransportCompany también tiene un campo obligatorio (ej: companyName)
                // transport.setCompanyName(request.getName());

                user = transport;
                user.setRole(Role.TRANSPORT);
                break;

            default:
                user = new Collector();
                user.setRole(Role.COLLECTOR);
                break;
        }

        // 3. Setear datos comunes de la clase abstracta User
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(request.getPassword()); // Nota: En el futuro aquí irá el BCrypt
        user.setPhone(request.getPhone());
        user.setTaxId(request.getTaxId());
        user.setVerificationFile(verificationDoc);

        // Configuración de Seguridad/KYB
        user.setStatus(UserStatus.PENDING);
        user.setEnabled(false);

        return userRepository.save(user);
    }

    // Métodos para el Administrador

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        user.setStatus(UserStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);
    }
}