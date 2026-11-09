package com.socialshuffle.controller;

import com.socialshuffle.model.AdminAccount;
import com.socialshuffle.repository.AdminRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller to identify and manage admin individuals using the database `admins` table.
 * If individual's role is admin in the admins table, admin view is authorized;
 * otherwise participant view is shown.
 */
@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminRepository adminRepository;

    public AdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /**
     * Get all active admins configured in the database admins table.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAdmins() {
        List<AdminAccount> admins = adminRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminAccount admin : admins) {
            if (admin.isActive()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", admin.getId());
                map.put("name", admin.getName());
                map.put("email", admin.getEmail());
                map.put("phone", admin.getPhone());
                map.put("role", admin.getRole() != null ? admin.getRole() : "admin");
                map.put("designation", admin.getDesignation());
                map.put("avatar", admin.getAvatar());
                map.put("bio", admin.getBio());
                map.put("active", admin.isActive());
                map.put("createdAt", admin.getCreatedAt());
                result.add(map);
            }
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Check if a logged in individual is an admin using their email or phone.
     * Query: /api/admins/check?identifier=parimalmshete@gmail.com
     * Returns: { "isAdmin": true, "role": "admin", "admin": { ... } }
     */
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
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("isAdmin", false);
            response.put("role", "participant");
            return ResponseEntity.ok(response);
        }

        String clean = query.trim().toLowerCase();

        // 1. Search by email
        Optional<AdminAccount> match = adminRepository.findByEmailIgnoreCase(clean);

        // 2. Search by phone
        if (match.isEmpty()) {
            match = adminRepository.findByPhone(clean);
        }

        // 3. Fallback active identifier query
        if (match.isEmpty()) {
            match = adminRepository.findActiveByIdentifier(clean);
        }

        if (match.isPresent() && match.get().isActive()) {
            AdminAccount admin = match.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("isAdmin", true);
            response.put("role", "admin");

            Map<String, Object> adminData = new LinkedHashMap<>();
            adminData.put("id", admin.getId());
            adminData.put("name", admin.getName());
            adminData.put("email", admin.getEmail());
            adminData.put("phone", admin.getPhone());
            adminData.put("role", admin.getRole());
            adminData.put("designation", admin.getDesignation());
            adminData.put("avatar", admin.getAvatar());
            adminData.put("bio", admin.getBio());

            response.put("admin", adminData);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> notFound = new LinkedHashMap<>();
        notFound.put("isAdmin", false);
        notFound.put("role", "participant");
        return ResponseEntity.ok(notFound);
    }

    /**
     * Get specific admin by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable("id") String id) {
        return adminRepository.findById(id)
                .map(admin -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", admin.getId());
                    map.put("name", admin.getName());
                    map.put("email", admin.getEmail());
                    map.put("phone", admin.getPhone());
                    map.put("role", admin.getRole());
                    map.put("designation", admin.getDesignation());
                    map.put("avatar", admin.getAvatar());
                    map.put("bio", admin.getBio());
                    map.put("active", admin.isActive());
                    return ResponseEntity.ok((Object) map);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Admin not found")));
    }
}
