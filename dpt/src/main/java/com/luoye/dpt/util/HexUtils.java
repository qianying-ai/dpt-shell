package com.luoye.dpt.util;

import java.util.Arrays;
import java.util.Locale;

public class HexUtils {
    public static String toHexArray(byte[] data){
        return Arrays.toString(toHexStringArray(data));
    }

    public static String toHexString(long l) {
        return "0x" + Long.toHexString(l);
    }

    public static String toHexString(byte[] data) {
        String[] hexStringArray = toHexStringArray(data);
        StringBuilder result = new StringBuilder();
        for (String s : hexStringArray) {
            result.append(s);
        }
        return result.toString();
    }

    /**
     * Decode a hex string (e.g. "0123ab") to bytes. Returns null on invalid input.
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || (hex.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static String[] toHexStringArray(byte[] data) {
        String[] array = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            int value = data[i];
            if(data[i] < 0){
                value = data[i] + 256;
            }
            array[i] = String.format(Locale.US,"%02x",value);
        }

        return array;
    }

}
