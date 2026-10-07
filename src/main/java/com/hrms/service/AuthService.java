package com.hrms.service;

import com.hrms.dto.LoginRequest;
import com.hrms.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);
}
