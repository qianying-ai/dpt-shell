package com.luoye.dpt.util;

import org.junit.Assert;
import org.junit.Test;
import javax.crypto.Cipher;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class CodeItemEncryptionTest {
    private byte[] decrypt(byte[] key, byte[] blob) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(CryptoUtils.hmacSha256(key, CryptoUtils.CODE_ITEM_DOMAIN), "AES"),
                new GCMParameterSpec(128, Arrays.copyOfRange(blob, 4, 16)));
        cipher.updateAAD(Arrays.copyOfRange(blob, 0, 4));
        return cipher.doFinal(Arrays.copyOfRange(blob, 16, blob.length));
    }

    @Test public void containerRoundTripsAndHasNativeGoldenVector() throws Exception {
        byte[] key = new byte[32], nonce = new byte[12];
        for (int i = 0; i < key.length; ++i) key[i] = (byte)i;
        byte[] plain = new byte[]{2, 0, 1, 0, 8, 0, 0, 0, 0, 0};
        byte[] blob = CryptoUtils.encryptCodeItemBlob(key, nonce, plain);
        Assert.assertArrayEquals(plain, decrypt(key, blob));
        // 留给 native 独立解析器的固定向量，不使用生产密钥。
        java.nio.file.Path out = java.nio.file.Paths.get("build/code-item-golden.bin");
        java.nio.file.Files.createDirectories(out.getParent());
        java.nio.file.Files.write(out, blob);
    }

    @Test public void modifiedHeaderNonceCiphertextAndTagAreRejected() throws Exception {
        byte[] key = new byte[32];
        byte[] blob = CryptoUtils.encryptCodeItemBlob(key, new byte[12], new byte[200]);
        for (int offset : new int[]{0, 4, 16, blob.length - 1}) {
            byte[] changed = blob.clone(); changed[offset] ^= 1;
            try { decrypt(key, changed); Assert.fail("篡改必须失败"); }
            catch (AEADBadTagException expected) { }
        }
    }

    @Test public void independentNonceChangesCiphertext() {
        byte[] first = new byte[12], second = new byte[12]; second[0] = 1;
        Assert.assertFalse(Arrays.equals(
                CryptoUtils.encryptCodeItemBlob(new byte[32], first, new byte[200]),
                CryptoUtils.encryptCodeItemBlob(new byte[32], second, new byte[200])));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidKeyIsRejected() {
        CryptoUtils.encryptCodeItemBlob(new byte[16], new byte[12], new byte[]{1});
    }
}
