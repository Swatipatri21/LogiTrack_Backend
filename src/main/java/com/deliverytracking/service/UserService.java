package com.deliverytracking.service;

import com.deliverytracking.dto.UserResponse;
import com.deliverytracking.entity.Hub;
import com.deliverytracking.entity.User;
import com.deliverytracking.enums.Role;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.HubRepository;
import com.deliverytracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final HubRepository  hubRepository;
    private final DeliveryStatusUpdateRepository deliveryStatusUpdateRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUserRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(role);
        userRepository.save(user);
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .hubId(user.getHub() != null ? user.getHub().getId() : null)
                .build();
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        deliveryStatusUpdateRepository.nullifyUpdatedBy(id);
        userRepository.delete(user);
    }

    
    @Transactional
    public UserResponse updateUser(Long id, String name, String email, String phone) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) user.setEmail(email);
        // Add this line:
//        if (phone != null && !phone.isBlank()) user.setPhone(phone); 
        
        userRepository.save(user);
        return mapToResponse(user);
    }
    
    public List<UserResponse> getUsersByHub(Long hubId) {
        return userRepository.findByHubId(hubId)
            .stream()
            .filter(u -> u.getRole() == Role.STAFF || u.getRole() == Role.HUB_MANAGER)
            .map(this::mapToResponse)   // use whatever mapper method you already have
            .toList();
    }
    
 // UserService.java — add this method
    @Transactional
    public UserResponse assignUserToHub(Long userId, Long hubId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Hub hub = hubRepository.findById(hubId)
            .orElseThrow(() -> new ResourceNotFoundException("Hub not found: " + hubId));

        user.setHub(hub);
        userRepository.save(user);

        return mapToResponse(user);
    }
    
    @Transactional
    public void assignHubToUser(Long userId, Hub hub) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Set the hub and ensure the role is updated to HUB_MANAGER
        user.setHub(hub);
        user.setRole(Role.HUB_MANAGER);
        
        userRepository.save(user);
    }
    
    public void deassignManager(Long hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
        
        User manager = hub.getManager();

        if (manager != null) {
            manager.setHub(null);   // remove hub reference
            userRepository.save(manager);
        }

        hub.setManager(null);
        hubRepository.save(hub);
    }
    
    
}
