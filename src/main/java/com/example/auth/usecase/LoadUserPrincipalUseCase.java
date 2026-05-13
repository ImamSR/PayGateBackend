package com.example.auth.usecase;

import com.example.auth.repository.UserRepository;
import com.example.auth.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class LoadUserPrincipalUseCase {

    private final UserRepository userRepository;

    public LoadUserPrincipalUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal execute(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(UserPrincipal::create)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
