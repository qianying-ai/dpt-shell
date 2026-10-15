package com.luoyesiqiu.shell;

import androidx.annotation.Keep;

/**
 * Created by luoyesiqiu
 */
@Keep
public class JniBridge {
    public static native void craoc(String applicationClassName);
    public static native void ia();
    public static native String rcf();
    public static native void cbde(ClassLoader targetClassLoader);
    public static native void rde(ClassLoader classLoader,String elementName);
    public static native String gap();
    public static native String gdp();
    public static native Object ra(String originApplicationClassName);
    public static native String rapn();
    public static native void clinit();

    public static void loadShellLibs(String workspacePath) {
        // 平台负责 ABI 选择和签名 APK 内映射；不再枚举、释放或加载应用可写副本。
        String name = Global.SHELL_SO_NAME;
        if (!name.startsWith("lib") || !name.endsWith(".so")) {
            throw new SecurityException("Invalid shell library name");
        }
        System.loadLibrary(name.substring(3, name.length() - 3));
    }

}
