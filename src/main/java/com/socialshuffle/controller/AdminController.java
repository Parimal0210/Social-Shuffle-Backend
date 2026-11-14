package com.socialshuffle.controller;

import com.socialshuffle.model.AdminAccount;
import com.socialshuffle.repository.AdminRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminRepository adminRepository;

    public AdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @GetMapping
    @Cacheable("admins")
    public ResponseEntity<List<Map<String, Object>>> getAllAdmins() {
        List<AdminAccount> admins = adminRepository.findByActiveTrue();
        List<Map<String, Object>> result = new ArrayList<>(admins.size());
        for (AdminAccount admin : admins) {
            result.add(toSafeAdminMap(admin));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAdminRole(
            @RequestParam(value = "identifier", required = false) String identifier,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone) {

        String query = identifier;
        if (query == null || query.trim().isEmpty()) {
            query = (email != null && !email.trim().isEmpty()) ? email : phone;
        }

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("isAdmin", false, "role", "participant"));
        }

        String clean = query.trim().toLowerCase();
        Optional<AdminAccount> match = adminRepository.findByEmailIgnoreCase(clean)
                .or(() -> adminRepository.findByPhone(clean))
                .or(() -> adminRepository.findActiveByIdentifier(clean));

        if (match.isPresent() && match.get().isActive()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("isAdmin", true);
            response.put("role", "admin");
            response.put("admin", toSafeAdminMap(match.get()));
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(Map.of("isAdmin", false, "role", "participant"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable("id") String id) {
        return adminRepository.findById(id)
                .map(admin -> ResponseEntity.ok((Object) toSafeAdminMap(admin)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Admin not found")));
    }

    private Map<String, Object> toSafeAdminMap(AdminAccount admin) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", admin.getId());
        map.put("name", admin.getName());
        map.put("email", admin.getEmail());
        map.put("phone", admin.getPhone());
        map.put("role", admin.getRole() != null ? admin.getRole() : "admin");
        map.put("designation", admin.getDesignation());
        map.put("avatar", admin.getAvatar());
        map.put("bio", admin.getBio());
        map.put("area", admin.getArea());
        map.put("active", admin.isActive());
        map.put("createdAt", admin.getCreatedAt());
        return map;
    }
}
