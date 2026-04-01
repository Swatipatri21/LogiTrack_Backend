package com.deliverytracking.service;

import com.deliverytracking.dto.AuthResponse;
import com.deliverytracking.dto.LoginRequest;
import com.deliverytracking.dto.RegisterRequest;
import com.deliverytracking.entity.Hub;
import com.deliverytracking.entity.User;
import com.deliverytracking.exception.EmailAlreadyExistsException;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.HubRepository;
import com.deliverytracking.repository.UserRepository;
import com.deliverytracking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final HubRepository hubRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

//    public AuthResponse register(RegisterRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
//        }
//        
//        Hub assignedHub = null;
//        if (request.getHubId() != null) {
//            assignedHub = hubRepository.findById(request.getHubId())
//                .orElseThrow(() -> new RuntimeException("Hub not found"));
//        }
//
//        User user = User.builder()
//                .name(request.getName())
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(request.getRole())
//                .hub(assignedHub)
//                .build();
//        
////        if (request.getHubId() != null) {
////            Hub hub = hubRepository.findById(request.getHubId())
////                .orElseThrow(() -> new ResourceNotFoundException("Hub not found"));
////            user.setHub(hub);
////        }
//
//        userRepository.save(user);
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
//        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
//
//        return AuthResponse.builder()
//                .token(token)
//                .email(user.getEmail())
//                .name(user.getName())
//                .role(user.getRole().name())
//                .message("User registered successfully")
//                .hubId(user.getHub() != null ? user.getHub().getId() : null)       // ADD
//                .hubName(user.getHub() != null ? user.getHub().getName() : null)
//                .build();
//    }
    
    
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        
        Hub assignedHub = null;
        if (request.getHubId() != null) {
            assignedHub = hubRepository.findById(request.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Hub not found"));
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .hub(assignedHub)
                .build();
        
        // Removed duplicate hub assignment - already set in builder

//        userRepository.save(user);
        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .id(user.getId())
                .role(user.getRole().name())
                .message("User registered successfully")
                .hubId(user.getHub() != null ? user.getHub().getId() : null)
                .hubName(user.getHub() != null ? user.getHub().getName() : null)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        
        emailService.sendLoginAlertEmail(user); 

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .id(user.getId())
                .role(user.getRole().name())
                .message("Login successful")
                .hubId(user.getHub() != null ? user.getHub().getId() : null)       // ADD
                .hubName(user.getHub() != null ? user.getHub().getName() : null)
                .build();
    }
}
