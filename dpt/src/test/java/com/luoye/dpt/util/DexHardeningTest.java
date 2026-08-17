package com.luoye.dpt.util;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DexHardeningTest {

    @Test
    public void perMethodKdfMatchesNativeCompileTimeVectors() {
        Assert.assertEquals(0xC42C5A1AA3820138L, DexUtils.deriveInsnsKeyStream(0, 0, 0));
        Assert.assertEquals(0x204391A6FD59956FL, DexUtils.deriveInsnsKeyStream(1, 0, 0));
        Assert.assertEquals(0xB0CAED0A7BF9C2C8L, DexUtils.deriveInsnsKeyStream(-1, 0, 0));
        Assert.assertEquals(0xF37B00A54C74820FL,
                DexUtils.deriveInsnsKeyStream(Integer.MIN_VALUE, 1, 1));
        Assert.assertEquals(0xEFA52F7ECD9E690AL,
                DexUtils.deriveInsnsKeyStream(0x12345678, 7, 42));
        Assert.assertEquals(0x73150763AB0415DFL,
                DexUtils.deriveInsnsKeyStream(0xDEADBEEF, 31, Integer.MAX_VALUE));
    }

    @Test
    public void hexDecoderAcceptsBothCasesAndRejectsMalformedInput() {
        Assert.assertArrayEquals(new byte[]{0x01, 0x23, (byte) 0xAB, (byte) 0xCD},
                HexUtils.hexToBytes("0123aBcD"));
        Assert.assertNull(HexUtils.hexToBytes(null));
        Assert.assertNull(HexUtils.hexToBytes("abc"));
        Assert.assertNull(HexUtils.hexToBytes("00xz"));
    }

    @Test
    public void dexZipBlobPrefixesIvAndRoundTripsPkcs7() throws Exception {
        byte[] key = new byte[32];
        byte[] iv = new byte[16];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 3 + 1);
        }
        for (int i = 0; i < iv.length; i++) {
            iv[i] = (byte) (0xA0 + i);
        }
        byte[] plainZip = "PK\u0003\u0004deterministic-test-zip".getBytes(StandardCharsets.UTF_8);

        byte[] blob = CryptoUtils.encryptDexZipBlob(key, iv, plainZip);

        Assert.assertNotNull(blob);
        Assert.assertArrayEquals(iv, Arrays.copyOfRange(blob, 0, 16));
        Assert.assertEquals(0, (blob.length - 16) % 16);
        Assert.assertFalse(Arrays.equals(Arrays.copyOfRange(blob, 16, 20),
                Arrays.copyOfRange(plainZip, 0, 4)));

        Cipher decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        Assert.assertArrayEquals(plainZip,
                decrypt.doFinal(Arrays.copyOfRange(blob, 16, blob.length)));
    }

    @Test
    public void dexZipBlobRejectsInvalidInputs() {
        Assert.assertNull(CryptoUtils.encryptDexZipBlob(new byte[32], new byte[15], new byte[]{1}));
        Assert.assertNull(CryptoUtils.encryptDexZipBlob(new byte[32], new byte[16], new byte[0]));
        Assert.assertNull(CryptoUtils.encryptDexZipBlob(new byte[31], new byte[16], new byte[]{1}));
    }
}
