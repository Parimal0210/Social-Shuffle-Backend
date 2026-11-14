package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "app_configs")
public class AppConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "updated_at", length = 50)
    private String updatedAt;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    public AppConfig() {
    }

    public AppConfig(String configKey, String configJson, String updatedAt, String updatedBy) {
        this.configKey = configKey;
        this.configJson = configJson;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
