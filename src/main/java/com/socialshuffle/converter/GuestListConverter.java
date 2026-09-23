package com.socialshuffle.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.GuestInfo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class GuestListConverter implements AttributeConverter<List<GuestInfo>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<GuestInfo> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<GuestInfo> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<GuestInfo>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
