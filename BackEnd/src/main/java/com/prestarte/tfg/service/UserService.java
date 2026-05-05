package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.EmailAlreadyExistsException;
import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.DBFileRepository;
import com.prestarte.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(RegistrationRequest request, MultipartFile file) throws IOException {
        // 1. Email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // 2. Validar archivo de verificación
        validateVerificationFile(file);

        // 3. Guardar documento de identidad
        DBFile verificationDoc = DBFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();
        dbFileRepository.save(verificationDoc);

        // 4. Instanciar la subclase correcta según rol
        User user = buildUserBySubclass(request);

        // 5. Datos comunes (la password se hashea aquí)
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setTaxId(request.getTaxId());
        user.setVerificationFile(verificationDoc);
        user.setStatus(UserStatus.PENDING);
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        sendWelcomeEmail(savedUser);
        return savedUser;
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));

        user.setStatus(UserStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);
        sendActivationEmail(user);
    }

    @Transactional
    public void rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));

        user.setStatus(UserStatus.REJECTED);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    // --- Helpers privados ---

    private User buildUserBySubclass(RegistrationRequest request) {
        String roleStr = request.getRole().toUpperCase();
        switch (roleStr) {
            case "FOUNDATION":
            case "MUSEUM":
                Foundation foundation = new Foundation();
                foundation.setInstitutionName(request.getName());
                foundation.setRole(Role.FOUNDATION);
                return foundation;

            case "TRANSPORT":
                TransportCompany transport = new TransportCompany();
                transport.setCompanyName(request.getName());
                transport.setRole(Role.TRANSPORT);
                return transport;

            case "COLLECTOR":
            default:
                Collector collector = new Collector();
                collector.setRole(Role.COLLECTOR);
                return collector;
        }
    }

    private void validateVerificationFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de verificación es obligatorio");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.equals("image/jpeg")
                        || contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo PDF, JPEG o PNG.");
        }
    }

    private void sendWelcomeEmail(User user) {
        String subject = "Bienvenido a Prestarte - Registro recibido";
        String body = "Hola " + user.getName() + ",\n\n" +
                "Gracias por registrarte. Tu perfil profesional está siendo revisado " +
                "por nuestro equipo de administración.\n\n" +
                "Te avisaremos por email cuando tu cuenta sea activada.\n\n" +
                "Un saludo,\nEl equipo de Prestarte.";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    private void sendActivationEmail(User user) {
        String subject = "Tu cuenta de Prestarte ha sido activada";
        String body = "Enhorabuena " + user.getName() + ",\n\n" +
                "Tu cuenta ha sido verificada y activada. Ya puedes iniciar sesión " +
                "y participar en la red de préstamos de obras de arte.\n\n" +
                "¡Bienvenido a bordo!\nEl equipo de Prestarte.";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }
}
