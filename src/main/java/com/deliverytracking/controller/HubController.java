package com.deliverytracking.controller;

import com.deliverytracking.dto.ApiResponse;
import com.deliverytracking.dto.HubRequest;
import com.deliverytracking.dto.HubResponse;
import com.deliverytracking.entity.Hub;
import com.deliverytracking.enums.Role;
import com.deliverytracking.repository.HubRepository;
import com.deliverytracking.repository.UserRepository;
import com.deliverytracking.service.GeoRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hubs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Hub Management", description = "Endpoints for managing hubs (ADMIN only)")
public class HubController {

    private final HubRepository hubRepository;
    private final GeoRoutingService geoRoutingService;
    private final UserRepository userRepository;

 // HubController.java — replace getAllHubs
    @GetMapping
    public ResponseEntity<ApiResponse<List<HubResponse>>> getAllHubs() {
        List<HubResponse> hubs = hubRepository.findAll().stream()
            .map(h -> HubResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .city(h.getCity())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .active(h.isActive())
                .managerId(h.getManager() != null ? h.getManager().getId() : null)
                .managerName(h.getManager() != null ? h.getManager().getName() : null)
                .managerEmail(h.getManager() != null ? h.getManager().getEmail() : null)
                .build())
            .toList();
        return ResponseEntity.ok(ApiResponse.success("Hubs retrieved", hubs));
    }

    @PostMapping
    @Operation(summary = "Create a new hub")
    public ResponseEntity<Hub> createHub(@Valid @RequestBody HubRequest request) {

        Hub hub = new Hub();
        hub.setName(request.getName());
        hub.setCity(request.getCity());
        hub.setActive(true);

        if (request.getLatitude() != 0 && request.getLongitude() != 0) {
            hub.setLatitude(request.getLatitude());
            hub.setLongitude(request.getLongitude());
        } else {
            double[] coords = geoRoutingService.geocodeAddress(request.getCity());
            hub.setLatitude(coords[0]);
            hub.setLongitude(coords[1]);
        }

        return ResponseEntity.ok(hubRepository.save(hub));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update hub details")
    public ResponseEntity<Hub> updateHub(@PathVariable Long id,
                                          @Valid @RequestBody HubRequest request) {
        Hub hub = hubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Hub not found: " + id));

        hub.setName(request.getName());
        hub.setCity(request.getCity());
        if (request.getLatitude() != 0) hub.setLatitude(request.getLatitude());
        if (request.getLongitude() != 0) hub.setLongitude(request.getLongitude());

        return ResponseEntity.ok(hubRepository.save(hub));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a hub")
    public ResponseEntity<String> deactivateHub(@PathVariable Long id) {
        Hub hub = hubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Hub not found: " + id));
        hub.setActive(false);
        hubRepository.save(hub);
        return ResponseEntity.ok("Hub deactivated.");
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Reactivate a hub")
    public ResponseEntity<String> activateHub(@PathVariable Long id) {
        Hub hub = hubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Hub not found: " + id));
        hub.setActive(true);
        hubRepository.save(hub);
        return ResponseEntity.ok("Hub activated.");
    }
    
 // In HubController.java — replace assignManager return type
    @PutMapping("/{id}/assign-manager/{userId}")
    public ResponseEntity<ApiResponse<String>> assignManager(
            @PathVariable Long id, @PathVariable Long userId) {

        Hub hub = hubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Hub not found: " + id));
        com.deliverytracking.entity.User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (!user.getRole().equals(Role.HUB_MANAGER)) {
            throw new RuntimeException("User does not have HUB_MANAGER role");
        }

        hub.setManager(user);
        user.setHub(hub);          // ← also set the other side
        hubRepository.save(hub);

        return ResponseEntity.ok(ApiResponse.success(
            "Manager assigned to " + hub.getName(), null));
    }
}