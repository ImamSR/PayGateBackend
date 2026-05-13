package com.auth.usecase;

import com.auth.repository.UserRepository;
import com.auth.security.UserPrincipal;
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
