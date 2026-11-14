package com.socialshuffle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.AppConfig;
import com.socialshuffle.repository.AppConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages persisted application settings (Communications / SMTP / Gmail / Razorpay)
 * in the PostgreSQL `app_configs` table with fast in-memory caching and env defaults.
 */
@Service
public class AppConfigService {

    public static final String KEY_COMMUNICATIONS = "communications_config";
    public static final String KEY_RAZORPAY = "razorpay_config";

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.host:smtp.gmail.com}")
    private String envSmtpHost;

    @Value("${app.mail.port:587}")
    private int envSmtpPort;

    @Value("${app.mail.username:}")
    private String envSmtpUser;

    @Value("${app.mail.password:}")
    private String envSmtpPass;

    @Value("${app.mail.from:}")
    private String envSmtpFrom;

    @Value("${app.mail.admin-email:parimalmshete@gmail.com}")
    private String envAdminEmail;

    @Value("${app.mail.resend-api-key:}")
    private String envResendApiKey;

    @Value("${app.razorpay.key-id:rzp_test_SocialShufflePune}")
    private String envRazorpayKeyId;

    @Value("${app.razorpay.key-secret:}")
    private String envRazorpayKeySecret;

    private volatile Map<String, Object> cachedCommConfig;
    private volatile Map<String, Object> cachedRazorpayConfig;

    public AppConfigService(AppConfigRepository appConfigRepository, ObjectMapper objectMapper) {
        this.appConfigRepository = appConfigRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getCommunicationsConfig() {
        if (cachedCommConfig != null) {
            return new LinkedHashMap<>(cachedCommConfig);
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("smtpHost", nonEmpty(envSmtpHost, "smtp.gmail.com"));
        defaults.put("smtpPort", envSmtpPort > 0 ? envSmtpPort : 587);
        defaults.put("smtpUser", nonEmpty(envSmtpUser, ""));
        defaults.put("smtpPass", nonEmpty(envSmtpPass, ""));
        defaults.put("smtpFrom", nonEmpty(envSmtpFrom, nonEmpty(envSmtpUser, "noreply@socialshufflepune.com")));
        defaults.put("adminNotificationEmail", nonEmpty(envAdminEmail, "parimalmshete@gmail.com"));
        defaults.put("resendApiKey", nonEmpty(envResendApiKey, ""));
        defaults.put("whatsappPhoneId", "");
        defaults.put("whatsappToken", "");
        defaults.put("gmailConnected", false);
        defaults.put("gmailEmail", "");
        defaults.put("gmailAccessToken", "");
        defaults.put("gmailExpiresAt", 0L);

        Optional<AppConfig> stored = appConfigRepository.findById(KEY_COMMUNICATIONS);
        if (stored.isPresent() && stored.get().getConfigJson() != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(stored.get().getConfigJson(), new TypeReference<>() {});
                for (Map.Entry<String, Object> e : parsed.entrySet()) {
                    if (e.getValue() != null) {
                        defaults.put(e.getKey(), e.getValue());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        cachedCommConfig = new LinkedHashMap<>(defaults);
        return defaults;
    }

    public Map<String, Object> saveCommunicationsConfig(Map<String, Object> updates, String updatedBy) {
        Map<String, Object> current = getCommunicationsConfig();
        if (updates != null) {
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (v == null) continue;
                // Do not overwrite real password/secrets with UI mask placeholder
                if (("smtpPass".equals(k) || "resendApiKey".equals(k) || "whatsappToken".equals(k))
                        && String.valueOf(v).contains("••••")) {
                    continue;
                }
                current.put(k, v);
            }
        }
        persistJsonConfig(KEY_COMMUNICATIONS, current, updatedBy);
        cachedCommConfig = new LinkedHashMap<>(current);
        return getCommunicationsConfig();
    }

    public Map<String, Object> getPublicCommunicationsConfig() {
        Map<String, Object> cfg = getCommunicationsConfig();
        String pass = str(cfg.get("smtpPass"));
        String resend = str(cfg.get("resendApiKey"));
        String waTok = str(cfg.get("whatsappToken"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("smtpHost", str(cfg.get("smtpHost")));
        out.put("smtpPort", cfg.get("smtpPort"));
        out.put("smtpUser", str(cfg.get("smtpUser")));
        out.put("smtpPassConfigured", !pass.isEmpty());
        out.put("smtpPass", pass.isEmpty() ? "" : "••••••••" + pass.substring(Math.max(0, pass.length() - 4)));
        out.put("smtpFrom", str(cfg.get("smtpFrom")));
        out.put("adminNotificationEmail", str(cfg.get("adminNotificationEmail")));
        out.put("resendConfigured", !resend.isEmpty());
        out.put("whatsappConfigured", !waTok.isEmpty());
        out.put("gmailConnected", Boolean.TRUE.equals(cfg.get("gmailConnected")));
        out.put("gmailEmail", str(cfg.get("gmailEmail")));
        return out;
    }

    public Map<String, Object> getRazorpayConfig() {
        if (cachedRazorpayConfig != null) {
            return new LinkedHashMap<>(cachedRazorpayConfig);
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("keyId", nonEmpty(envRazorpayKeyId, "rzp_test_SocialShufflePune"));
        defaults.put("keySecret", nonEmpty(envRazorpayKeySecret, ""));
        defaults.put("currency", "INR");
        defaults.put("enabled", true);
        defaults.put("isLive", nonEmpty(envRazorpayKeyId, "").startsWith("rzp_live_"));
        defaults.put("companyName", "Social Shuffle Pune");
        defaults.put("themeColor", "#FF751F");

        Optional<AppConfig> stored = appConfigRepository.findById(KEY_RAZORPAY);
        if (stored.isPresent() && stored.get().getConfigJson() != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(stored.get().getConfigJson(), new TypeReference<>() {});
                for (Map.Entry<String, Object> e : parsed.entrySet()) {
                    if (e.getValue() != null) {
                        defaults.put(e.getKey(), e.getValue());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        cachedRazorpayConfig = new LinkedHashMap<>(defaults);
        return defaults;
    }

    public Map<String, Object> saveRazorpayConfig(Map<String, Object> updates, String updatedBy) {
        Map<String, Object> current = getRazorpayConfig();
        if (updates != null) {
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (v == null) continue;
                if ("keySecret".equals(k) && String.valueOf(v).contains("••••")) continue;
                current.put(k, v);
            }
        }
        String keyId = str(current.get("keyId"));
        current.put("isLive", keyId.startsWith("rzp_live_"));
        persistJsonConfig(KEY_RAZORPAY, current, updatedBy);
        cachedRazorpayConfig = new LinkedHashMap<>(current);
        return getPublicRazorpayConfig();
    }

    public Map<String, Object> getPublicRazorpayConfig() {
        Map<String, Object> cfg = getRazorpayConfig();
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("keyId", str(cfg.get("keyId")));
        safe.put("currency", nonEmpty(str(cfg.get("currency")), "INR"));
        safe.put("enabled", !Boolean.FALSE.equals(cfg.get("enabled")));
        safe.put("isLive", Boolean.TRUE.equals(cfg.get("isLive")));
        safe.put("companyName", nonEmpty(str(cfg.get("companyName")), "Social Shuffle Pune"));
        safe.put("themeColor", nonEmpty(str(cfg.get("themeColor")), "#FF751F"));
        safe.put("keySecretConfigured", !str(cfg.get("keySecret")).isEmpty());
        return safe;
    }

    private void persistJsonConfig(String key, Map<String, Object> data, String updatedBy) {
        try {
            String json = objectMapper.writeValueAsString(data);
            appConfigRepository.save(new AppConfig(key, json, Instant.now().toString(), nonEmpty(updatedBy, "Admin")));
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist configuration " + key, e);
        }
    }

    private String nonEmpty(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : fallback;
    }

    private String str(Object obj) {
        return obj == null ? "" : String.valueOf(obj).trim();
    }
}
