package com.telecom.customer.application;

import com.telecom.customer.domain.entity.UserAccount;
import com.telecom.customer.domain.entity.Client;
import com.telecom.customer.domain.repository.UserAccountRepository;
import com.telecom.customer.domain.repository.ClientRepository;
import com.telecom.customer.infrastructure.security.JwtService;
import com.telecom.customer.web.dto.LoginRequest;
import com.telecom.customer.web.dto.LoginResponse;
import com.telecom.customer.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserAccountRepository userAccountRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.email())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        if (!user.isEnabled()) {
            throw new RuntimeException("Account disabled");
        }
        
        user.recordLogin();
        userAccountRepository.save(user);
        
        String token = jwtService.generateToken(user);
        
        log.info("User logged in: {}", user.getEmail());
        
        return new LoginResponse(
            token,
            user.getEmail(),
            user.getRole().name(),
            user.getClient() != null ? user.getClient().getId() : null
        );
    }
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create client if clientId not provided
        Client client = null;
        if (request.clientId() != null) {
            client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        }
        
        UserAccount user = UserAccount.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(UserAccount.Role.valueOf(request.role()))
            .client(client)
            .enabled(true)
            .build();
        
        user = userAccountRepository.save(user);
        
        String token = jwtService.generateToken(user);
        
        log.info("User registered: {}", user.getEmail());
        
        return new LoginResponse(
            token,
            user.getEmail(),
            user.getRole().name(),
            client != null ? client.getId() : null
        );
    }
}
