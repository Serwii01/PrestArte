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
    private final EmailService emailService; // Injected for notifications

    @Transactional
    public User registerUser(RegistrationRequest request, MultipartFile file) throws IOException {
        // 1. Save identity document
        DBFile verificationDoc = DBFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .build();
        dbFileRepository.save(verificationDoc);

        // 2. Instantiate the correct subclass based on role
        User user;
        String roleStr = request.getRole() != null ? request.getRole().toUpperCase() : "COLLECTOR";

        switch (roleStr) {
            case "MUSEUM":
            case "FOUNDATION":
                Foundation foundation = new Foundation();
                foundation.setInstitutionName(request.getName());
                foundation.setAddress("Pending completion");
                foundation.setCity("Pending completion");
                user = foundation;
                user.setRole(Role.FOUNDATION);
                break;

            case "TRANSPORT":
                TransportCompany transport = new TransportCompany();
                // If TransportCompany has specific required fields, set them here
                user = transport;
                user.setRole(Role.TRANSPORT);
                break;

            default:
                user = new Collector();
                user.setRole(Role.COLLECTOR);
                break;
        }

        // 3. Set common fields
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setTaxId(request.getTaxId());
        user.setVerificationFile(verificationDoc);

        // 4. Security status
        user.setStatus(UserStatus.PENDING);
        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        // 5. Send welcome email (notifying that account is pending approval)
        sendWelcomeEmail(savedUser);

        return savedUser;
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus(UserStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);

        // Send activation email
        sendActivationEmail(user);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    // --- Private Helper Methods for Emails ---

    private void sendWelcomeEmail(User user) {
        String subject = "Welcome to Prestarte - Registration Received";
        String body = "Hello " + user.getName() + ",\n\n" +
                "Thank you for registering on our platform. Your professional profile is currently " +
                "under review by our administration team to ensure the safety of our art community.\n\n" +
                "We will notify you via email as soon as your account is activated.\n\n" +
                "Best regards,\nThe Prestarte Team.";

        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    private void sendActivationEmail(User user) {
        String subject = "Prestarte Account Activated";
        String body = "Congratulations " + user.getName() + "!\n\n" +
                "Your account has been successfully verified and activated. You can now log in " +
                "and participate in our art loan network.\n\n" +
                "Welcome aboard!\nThe Prestarte Team.";

        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }
}