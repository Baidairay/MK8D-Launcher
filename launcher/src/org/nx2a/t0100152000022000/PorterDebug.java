package org.nx2a.t0100152000022000;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PorterDebug {
    private static final String NAME = "porter_debug.txt";
    private static final Object LOCK = new Object();
    private static Context appCtx = null;

    public static void init(Context context) {
        if (context != null) {
            appCtx = context.getApplicationContext();
        }
    }

    public static File file() {
        if (appCtx == null) {
            return null;
        }
        return new File(appCtx.getFilesDir(), NAME);
    }

    public static void clear() {
        synchronized (LOCK) {
            File file = file();
            if (file != null) {
                file.delete();
            }
        }
    }

    public static void log(String str) {
        try {
            File file = file();
            if (file == null) {
                return;
            }
            String str2 = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date()) + " " + str + "\n";
            synchronized (LOCK) {
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                fileOutputStream.write(str2.getBytes("UTF-8"));
                fileOutputStream.close();
            }
            Log.d("PorterDebug", str);
        } catch (Exception e) {
        }
    }

    public static void logStatic(String str) {
        log(str);
    }
}
