package com.luoye.dpt.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    public static final String CODE_ITEM_DOMAIN = "qy-dpt-code-item/v1";

    /** 指令容器独立派生密钥；魔数与版本参与认证，随机 nonce 每包生成。 */
    public static byte[] encryptCodeItemBlob(byte[] rootKey, byte[] nonce, byte[] plain) {
        if (rootKey == null || rootKey.length != 32 || nonce == null || nonce.length != 12
                || plain == null || plain.length == 0) {
            throw new IllegalArgumentException("invalid code item input");
        }
        try {
            byte[] magic = new byte[] {'D', 'P', 'C', '1'};
            byte[] key = hmacSha256(rootKey, CODE_ITEM_DOMAIN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(magic);
            byte[] sealed = cipher.doFinal(plain);
            byte[] blob = new byte[16 + sealed.length];
            System.arraycopy(magic, 0, blob, 0, 4);
            System.arraycopy(nonce, 0, blob, 4, 12);
            System.arraycopy(sealed, 0, blob, 16, sealed.length);
            return blob;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("code item encryption failed", e);
        }
    }
    public static final String RC4Transform = "RC4";
    private static final String HMAC_SHA256 = "HmacSHA256";

    public static byte[] rc4Crypt(byte[] key, byte[] in) {
        try {
            Cipher cipher = Cipher.getInstance(RC4Transform);
            SecretKeySpec spec = new SecretKeySpec(key, RC4Transform);
            cipher.init(Cipher.ENCRYPT_MODE,spec);
            return cipher.doFinal(in);
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Derive AES-256 key by HMAC-SHA256(randomKey, UTF-8(keyMaterial)).
     */
    public static byte[] hmacSha256(byte[] key, String keyMaterial) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("hmac key is empty");
        }
        if (keyMaterial == null || keyMaterial.isEmpty()) {
            throw new IllegalArgumentException("key material is empty");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            byte[] result = mac.doFinal(keyMaterial.getBytes(StandardCharsets.UTF_8));
            if (result == null || result.length != 32) {
                throw new IllegalStateException("unexpected hmac length");
            }
            return result;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("hmac-sha256 failed", e);
        }
    }

    public static byte[] aesEncrypt(byte[] key, byte[] iv, byte[] in) {
        try {
            Key secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec,ivParameterSpec);
            return cipher.doFinal(in);
        }
        catch (Exception e){
        }
        return null;
    }

    /**
     * Encrypt the dex ZIP into the on-disk blob format: IV || AES-CBC ciphertext.
     */
    public static byte[] encryptDexZipBlob(byte[] key, byte[] iv, byte[] plainZip) {
        if (iv == null || iv.length != 16 || plainZip == null || plainZip.length == 0) {
            return null;
        }
        byte[] cipher = aesEncrypt(key, iv, plainZip);
        if (cipher == null || cipher.length == 0) {
            return null;
        }
        byte[] blob = new byte[iv.length + cipher.length];
        System.arraycopy(iv, 0, blob, 0, iv.length);
        System.arraycopy(cipher, 0, blob, iv.length, cipher.length);
        return blob;
    }
}
