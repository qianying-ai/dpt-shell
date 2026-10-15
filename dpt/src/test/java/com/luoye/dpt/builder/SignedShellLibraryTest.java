package com.luoye.dpt.builder;

import org.junit.Assert;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;

public class SignedShellLibraryTest {
    @Test public void preservesManifestAndMovesBothArmsToPlatformPaths() throws Exception {
        Path root = Files.createTempDirectory("signed-shell-test");
        try {
            byte[] manifest = new byte[]{1, 2, 3, 4};
            Files.write(root.resolve("AndroidManifest.xml"), manifest);
            Files.createDirectories(root.resolve("assets/vwwwwwvwww/arm"));
            Files.createDirectories(root.resolve("assets/vwwwwwvwww/arm64"));
            Files.write(root.resolve("assets/vwwwwwvwww/arm/libshell.so"), new byte[]{1});
            Files.write(root.resolve("assets/vwwwwwvwww/arm64/libshell.so"), new byte[]{2});
            Apk apk = new Apk.Builder().build();
            apk.setExtractNativeLibs(root.toString());
            apk.moveShellLibrariesToSignedLibDir(root.toString());
            Assert.assertArrayEquals(manifest, Files.readAllBytes(root.resolve("AndroidManifest.xml")));
            Assert.assertArrayEquals(new byte[]{1}, Files.readAllBytes(root.resolve("lib/armeabi-v7a/libshell.so")));
            Assert.assertArrayEquals(new byte[]{2}, Files.readAllBytes(root.resolve("lib/arm64-v8a/libshell.so")));
            Assert.assertFalse(Files.exists(root.resolve("assets/vwwwwwvwww")));
            Path output = Files.createTempFile("signed-shell", ".apk");
            try {
                com.luoye.dpt.util.ZipUtils.zip(root.toString(), output.toString(), false);
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(output.toFile())) {
                    Assert.assertEquals(java.util.zip.ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libshell.so").getMethod());
                    Assert.assertEquals(java.util.zip.ZipEntry.STORED, zip.getEntry("lib/armeabi-v7a/libshell.so").getMethod());
                }
            } finally { Files.deleteIfExists(output); }
        } finally { com.luoye.dpt.util.FileUtils.deleteRecurse(root.toFile()); }
    }

    @Test(expected = IllegalStateException.class)
    public void refusesToOverwriteHostLibrary() throws Exception {
        Path root = Files.createTempDirectory("signed-shell-collision");
        try {
            Files.createDirectories(root.resolve("assets/vwwwwwvwww/arm64"));
            Files.createDirectories(root.resolve("lib/arm64-v8a"));
            Files.write(root.resolve("assets/vwwwwwvwww/arm64/libshell.so"), new byte[]{1});
            Files.write(root.resolve("lib/arm64-v8a/libshell.so"), new byte[]{2});
            new Apk.Builder().build().moveShellLibrariesToSignedLibDir(root.toString());
        } finally { com.luoye.dpt.util.FileUtils.deleteRecurse(root.toFile()); }
    }
}
