package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService es) {
        encryptionService = es;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return dbData;
        try {
            return encryptionService.decrypt(dbData);
        } catch (RuntimeException e) {
            try {
                return encryptionService.decryptLegacy(dbData);
            } catch (RuntimeException e2) {
                return dbData;
            }
        }
    }
}