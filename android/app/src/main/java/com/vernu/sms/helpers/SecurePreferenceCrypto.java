package com.vernu.sms.helpers;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurePreferenceCrypto {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "textbee_secure_preferences_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PREFIX = "enc:v1:";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String encrypt(String plaintext) throws Exception {
        if (plaintext == null) {
            return null;
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return PREFIX
                + Base64.encodeToString(iv, Base64.NO_WRAP)
                + ":"
                + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
    }

    public static String decrypt(String storedValue) throws Exception {
        if (!isEncrypted(storedValue)) {
            return storedValue;
        }

        String payload = storedValue.substring(PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted preference format");
        }

        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(keySpec);
        return keyGenerator.generateKey();
    }
}
