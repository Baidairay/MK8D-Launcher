package org.nx2a.t0100152000022000;

import java.util.Map;

/* loaded from: classes.dex */
public final class PorterBackend {

    public interface NativePorter {
        void extract(int i, int i2, String str, String str2, ProgressCallback progressCallback) throws PorterException;

        String validateTitles(int i, int i2, String str) throws PorterException;
    }

    public interface ProgressCallback {
        boolean isCancelled();

        void onProgress(String str, int i, String str2);
    }

    public interface PythonBackend {
        void runStage(String str, Map<String, String> map, ProgressCallback progressCallback) throws PorterException;
    }

    private PorterBackend() {
    }

    public static class PorterException extends Exception {
        public PorterException(String str) {
            super(str);
        }

        public PorterException(String str, Throwable th) {
            super(str, th);
        }
    }
}
