package org.nx2a.t0100152000022000;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.nx2a.t0100152000022000.PorterBackend;

/* loaded from: classes.dex */
public class PythonBackendImpl implements PorterBackend.PythonBackend {
    private static boolean loaded;
    private final Context context;
    private final String nativeLibDir;

    private native void runStageNative(String str, Map<String, String> map, PorterBackend.ProgressCallback progressCallback) throws PorterBackend.PorterException;

    static {
        loaded = false;
        try {
            System.loadLibrary("porter_python");
            loaded = true;
        } catch (Throwable th) {
            loaded = false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public PythonBackendImpl(Context context, String str) {
        this.context = context;
        this.nativeLibDir = str;
    }

    @Override // org.nx2a.t0100152000022000.PorterBackend.PythonBackend
    public void runStage(String str, Map<String, String> map, PorterBackend.ProgressCallback progressCallback) throws PorterBackend.PorterException {
        HashMap hashMap = new HashMap(map);
        hashMap.put("pythonDir", ensurePythonDir());
        hashMap.put("recompiler", this.nativeLibDir + "/librecompiler_android_c.so");
        preloadNativeLibs();
        runStageNative(str, hashMap, progressCallback);
    }

    private void preloadNativeLibs() {
        PorterDebug.init(this.context);
        String[] strArr = {"c++_shared", "shader_recompiler_android", "recompiler_android_c", "capstone"};
        for (int i = 0; i < 4; i++) {
            try {
                System.loadLibrary(strArr[i]);
                PorterDebug.log("service: preload " + strArr[i] + " ok");
            } catch (Throwable th) {
                PorterDebug.log("service: preload " + strArr[i] + " FAILED: " + th.getMessage());
            }
        }
    }

    private String ensurePythonDir() throws PorterBackend.PorterException {
        File file = new File(this.context.getFilesDir(), "porter_python");
        File file2 = new File(file, "VERSION");
        if (isMarkerOk(file2)) {
            return file.getAbsolutePath();
        }
        deleteRecursive(file);
        file.mkdirs();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(this.context.getAssets().open("porter_python.zip"));
            byte[] bArr = new byte[65536];
            while (true) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry != null) {
                    String name = nextEntry.getName();
                    if (isSafeEntry(name)) {
                        if (name.startsWith("python3.14/")) {
                            name = "lib/" + name;
                        } else if (name.startsWith("lib-dynload/")) {
                            name = "lib/python3.14/" + name;
                        }
                        File file3 = new File(file, name);
                        if (nextEntry.isDirectory()) {
                            file3.mkdirs();
                        } else {
                            file3.getParentFile().mkdirs();
                            FileOutputStream fileOutputStream = new FileOutputStream(file3);
                            while (true) {
                                int read = zipInputStream.read(bArr);
                                if (read <= 0) {
                                    break;
                                }
                                fileOutputStream.write(bArr, 0, read);
                            }
                            fileOutputStream.close();
                        }
                        zipInputStream.closeEntry();
                    }
                } else {
                    zipInputStream.close();
                    FileOutputStream fileOutputStream2 = new FileOutputStream(file2);
                    fileOutputStream2.write("3".getBytes("UTF-8"));
                    fileOutputStream2.close();
                    return file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            deleteRecursive(file);
            throw new PorterBackend.PorterException("cannot extract porter_python.zip: " + e.getMessage());
        }
    }

    private static boolean isMarkerOk(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bArr = new byte[16];
            int read = fileInputStream.read(bArr);
            fileInputStream.close();
            if (read > 0) {
                return new String(bArr, 0, read, "UTF-8").trim().equals("8");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSafeEntry(String str) {
        if (str == null || str.length() == 0 || str.charAt(0) == '/') {
            return false;
        }
        for (String str2 : str.split("/")) {
            if (str2.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private static void deleteRecursive(File file) {
        File[] listFiles;
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
            for (File file2 : listFiles) {
                deleteRecursive(file2);
            }
        }
        file.delete();
    }
}
