package com.himalaya.sabor.service;

import com.himalaya.sabor.dto.RegisterDto;
import com.himalaya.sabor.model.Role;
import com.himalaya.sabor.model.User;
import com.himalaya.sabor.model.UserStatus;
import com.himalaya.sabor.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public User registerUser(RegisterDto dto, String baseUrl) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado en el sistema");
        }

        User user = new User();
        user.setName(dto.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setAddress(dto.getAddress().trim());
        user.setPhone(dto.getPhone().trim());
        user.setRole(Role.CLIENT);
        user.setStatus(UserStatus.PENDIENTE_ACTIVACION);

        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);
        user.setActivationExpiresAt(LocalDateTime.now().plusHours(24));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        String activationLink = baseUrl + "/activar-cuenta?token=" + token;
        emailService.sendActivationEmail(savedUser.getName(), savedUser.getEmail(), activationLink);

        return savedUser;
    }

    @Transactional
    public ActivationResult activateAccount(String token) {
        if (token == null || token.isBlank()) {
            return new ActivationResult(false, "Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace");
        }

        Optional<User> optionalUser = userRepository.findByActivationToken(token);
        if (optionalUser.isEmpty()) {
            return new ActivationResult(false, "Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace");
        }

        User user = optionalUser.get();
        if (user.getActivationExpiresAt() != null && user.getActivationExpiresAt().isBefore(LocalDateTime.now())) {
            return new ActivationResult(false, "Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace");
        }

        user.setStatus(UserStatus.ACTIVA);
        user.setActivationToken(null);
        user.setActivationExpiresAt(null);
        userRepository.save(user);

        return new ActivationResult(true, "Cuenta activada con éxito. Ya puede iniciar sesión");
    }

    @Transactional
    public boolean resendActivation(String email, String baseUrl) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getStatus() == UserStatus.PENDIENTE_ACTIVACION) {
                String token = UUID.randomUUID().toString();
                user.setActivationToken(token);
                user.setActivationExpiresAt(LocalDateTime.now().plusHours(24));
                userRepository.save(user);

                String activationLink = baseUrl + "/activar-cuenta?token=" + token;
                emailService.sendActivationEmail(user.getName(), user.getEmail(), activationLink);
                return true;
            }
        }
        return false;
    }

    public static class ActivationResult {
        private final boolean success;
        private final String message;

        public ActivationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
