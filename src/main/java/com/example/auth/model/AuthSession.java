package com.example.auth.model;

import com.example.auth.dto.LoginResponse;

public record AuthSession(LoginResponse loginResponse, String refreshToken) {
}
