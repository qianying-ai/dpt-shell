package com.luoye.dpt.config;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class ShellConfigDexZipKeyTest {

    @Test
    public void jsonCarriesDexZipKeyOnlyWhenConfigured() {
        ShellConfig config = ShellConfig.getInstance();
        String previousPackage = config.getShellPackageName();
        String previousKey = config.getDexZipKey();
        try {
            config.setShellPackageName("com.example.shell");
            config.setDexZipKey(null);
            Assert.assertFalse(new JSONObject(config.toJson()).has("dex_zip_key"));

            String key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
            config.setDexZipKey(key);
            Assert.assertEquals(key, new JSONObject(config.toJson()).getString("dex_zip_key"));
        } finally {
            config.setShellPackageName(previousPackage);
            config.setDexZipKey(previousKey);
        }
    }
}
