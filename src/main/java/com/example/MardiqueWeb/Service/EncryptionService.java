package com.example.MardiqueWeb.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_LEN = 128;
    private final SecretKeySpec keySpec;
    private final SecretKeySpec legacyKeySpec;
    private final IvParameterSpec legacyIvSpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.key}") String rawKey) {
        this.keySpec = new SecretKeySpec(deriveKey(rawKey), "AES");

        byte[] rawBytes = rawKey.getBytes();
        byte[] keyBytes = new byte[16];
        System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 16));
        this.legacyKeySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] ivBytes = new byte[16];
        System.arraycopy(rawBytes, 0, ivBytes, 0, Math.min(rawBytes.length, 16));
        this.legacyIvSpec = new IvParameterSpec(ivBytes);
    }

    private byte[] deriveKey(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes("UTF-8"));
            byte[] key = new byte[16];
            System.arraycopy(hash, 0, key, 0, 16);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    public String encrypt(String data) {
        if (data == null || data.isEmpty()) return data;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] ciphertext = cipher.doFinal(data.getBytes("UTF-8"));
            byte[] combined = new byte[GCM_IV_LEN + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LEN);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LEN, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            if (decoded.length < GCM_IV_LEN) return encrypted;
            byte[] iv = new byte[GCM_IV_LEN];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LEN);
            byte[] ciphertext = new byte[decoded.length - GCM_IV_LEN];
            System.arraycopy(decoded, GCM_IV_LEN, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LEN, iv));
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String decryptLegacy(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, legacyKeySpec, legacyIvSpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Legacy decryption failed", e);
        }
    }
}