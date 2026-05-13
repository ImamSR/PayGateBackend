package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.model.UserRole;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.UserPrincipal;
import com.example.payment.entity.PaymentAccount;
import com.example.payment.repository.PaymentAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthCommandHandler {

    private final UserRepository userRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthCommandHandler(
            UserRepository userRepository,
            PaymentAccountRepository paymentAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerBasicUser(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken!");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use!");
        }

        User user = new User(username, passwordEncoder.encode(rawPassword), email, UserRole.USER);
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        try {
            paymentAccountRepository.save(new PaymentAccount(savedUser.getId(), savedUser.getUsername()));
        } catch (RuntimeException exception) {
            userRepository.deleteById(savedUser.getId());
            throw exception;
        }

        return savedUser;
    }

    public UserPrincipal loadPrincipal(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(UserPrincipal::create)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
