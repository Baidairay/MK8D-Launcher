package org.nx2a.t0100152000022000;

import org.nx2a.t0100152000022000.PorterBackend;

/* loaded from: classes.dex */
public class NativePorterImpl implements PorterBackend.NativePorter {
    private static boolean loaded;

    private native void extractNative(int i, int i2, String str, String str2, PorterBackend.ProgressCallback progressCallback) throws PorterBackend.PorterException;

    private native String validateTitlesNative(int i, int i2) throws PorterBackend.PorterException;

    static {
        loaded = false;
        try {
            System.loadLibrary("porter_native_jni");
            loaded = true;
        } catch (Throwable th) {
            loaded = false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    @Override // org.nx2a.t0100152000022000.PorterBackend.NativePorter
    public String validateTitles(int i, int i2, String str) throws PorterBackend.PorterException {
        return validateTitlesNative(i, i2);
    }

    @Override // org.nx2a.t0100152000022000.PorterBackend.NativePorter
    public void extract(int i, int i2, String str, String str2, PorterBackend.ProgressCallback progressCallback) throws PorterBackend.PorterException {
        extractNative(i, i2, str, str2, progressCallback);
    }
}
