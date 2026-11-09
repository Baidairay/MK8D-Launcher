package org.nx2a.t0100152000022000;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import org.nx2a.t0100152000022000.PorterBackend;

/* loaded from: classes.dex */
public class PorterService extends Service {
    public static final String ACTION_CANCEL = "org.nx2a.t0100152000022000.PORTER_CANCEL";
    public static final String ACTION_PROGRESS = "org.nx2a.t0100152000022000.PORTER_PROGRESS";
    private static final String CHANNEL_ID = "porter";
    private static final String DONE_CHANNEL_ID = "porter_done";
    public static final String EXTRA_BASE_URI = "base_uri";
    public static final String EXTRA_DEBUG = "debug_log";
    public static final String EXTRA_DONE = "done";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_KEYS_URI = "keys_uri";
    public static final String EXTRA_LOG = "log";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_REPLACE = "replace";
    public static final String EXTRA_STAGE = "stage";
    public static final String EXTRA_UPDATE_URI = "update_uri";
    private static final int NOTIF_ID = 1347375700;
    private static final long REQUIRED_FREE = 26843545600L;
    public static final String RESULT_FILE = "porter_result.txt";
    private static final String TAG = "PorterService";
    private PorterBackend.NativePorter nativePorter;
    private PorterBackend.PythonBackend pythonBackend;
    private File root;
    private String validatedCnmtVersion;
    private String validatedTitleId;
    private PowerManager.WakeLock wakeLock;
    private File workDir;
    private Thread worker;
    private static final List<ProgressListener> listeners = new ArrayList();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String[] STAGES = {"PREFLIGHT", "EXTRACT", "REWRITE", "SPLIT", "TRANSLATE", "PACKAGE", "SWAP"};
    private static final int[] STAGE_WEIGHTS = {2, 28, 5, 30, 20, 12, 3};
    private static final String[] PACK_ENTRIES = {"RomFS", "mods", "shaders.dmsv", "env.txt", "nx2a-data.json"};
    private volatile boolean running = false;
    private volatile boolean cancelled = false;
    private final StringBuilder logTrail = new StringBuilder();
    private int logTrailLines = 0;
    private String failureLogPath = null;
    private final PorterBackend.ProgressCallback progressCb = new PorterBackend.ProgressCallback() { // from class: org.nx2a.t0100152000022000.PorterService.3
        @Override // org.nx2a.t0100152000022000.PorterBackend.ProgressCallback
        public void onProgress(String str, int i, String str2) {
            PorterDebug.log("service: onProgress " + str + " " + i + "% " + ((str2 == null || str2.length() <= 80) ? str2 : str2.substring(0, 80)));
            PorterService.this.broadcast(str, i, str2, false, null, null);
            PorterService.this.notifyProgress(str, i, str2);
        }

        @Override // org.nx2a.t0100152000022000.PorterBackend.ProgressCallback
        public boolean isCancelled() {
            return PorterService.this.cancelled;
        }
    };

    public interface ProgressListener {
        void onProgress(String str, int i, String str2, boolean z, String str3, String str4, String str5);
    }

    public static void addListener(ProgressListener progressListener) {
        synchronized (listeners) {
            if (!listeners.contains(progressListener)) {
                listeners.add(progressListener);
            }
        }
    }

    public static void removeListener(ProgressListener progressListener) {
        synchronized (listeners) {
            listeners.remove(progressListener);
        }
    }

    private void notifyListeners(final String str, final int i, final String str2, final boolean z, final String str3, final String str4, final String str5) {
        final ArrayList arrayList;
        synchronized (listeners) {
            arrayList = new ArrayList(listeners);
        }
        if (arrayList.isEmpty()) {
            return;
        }
        mainHandler.post(new Runnable() { // from class: org.nx2a.t0100152000022000.PorterService.1
            @Override // java.lang.Runnable
            public void run() {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    try {
                        ((ProgressListener) it.next()).onProgress(str, i, str2, z, str3, str4, str5);
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    private int overallPercent(String str, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < STAGES.length; i3++) {
            if (str != null && STAGES[i3].equalsIgnoreCase(str)) {
                if (i < 0) {
                    i = 0;
                } else if (i > 100) {
                    i = 100;
                }
                int i4 = i2 + ((i * STAGE_WEIGHTS[i3]) / 100);
                if (i4 > 100) {
                    return 100;
                }
                return i4;
            }
            i2 += STAGE_WEIGHTS[i3];
        }
        if (i < 0) {
            return 0;
        }
        if (i > 100) {
            return 100;
        }
        return i;
    }

    private void createBackends() {
        try {
            if (NativePorterImpl.isLoaded()) {
                this.nativePorter = new NativePorterImpl();
            }
        } catch (Throwable th) {
            Log.w(TAG, "native backend unavailable", th);
            this.nativePorter = null;
        }
        try {
            if (PythonBackendImpl.isLoaded()) {
                this.pythonBackend = new PythonBackendImpl(this, getApplicationInfo().nativeLibraryDir);
            }
        } catch (Throwable th2) {
            Log.w(TAG, "python backend unavailable", th2);
            this.pythonBackend = null;
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = getFilesDir();
        }
        Lang.loadFromEnv(this, externalFilesDir);
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            PorterDebug.init(this);
            PorterDebug.log("service: ACTION_CANCEL received (cancelled=true)");
            this.cancelled = true;
            return 2;
        }
        if (this.running || intent == null) {
            return 2;
        }
        this.running = true;
        this.cancelled = false;
        startForeground(NOTIF_ID, buildNotification(Lang.t("install_nsp"), "Starting...", 0), 1);
        PowerManager powerManager = (PowerManager) getSystemService("power");
        if (powerManager != null) {
            this.wakeLock = powerManager.newWakeLock(1, "MKart8Port:porter");
            this.wakeLock.acquire();
        }
        final Uri uri = (Uri) intent.getParcelableExtra(EXTRA_BASE_URI);
        final Uri uri2 = (Uri) intent.getParcelableExtra(EXTRA_UPDATE_URI);
        final Uri uri3 = (Uri) intent.getParcelableExtra(EXTRA_KEYS_URI);
        final boolean booleanExtra = intent.getBooleanExtra(EXTRA_REPLACE, false);
        this.failureLogPath = null;
        this.logTrail.setLength(0);
        this.logTrailLines = 0;
        new File(getFilesDir(), RESULT_FILE).delete();
        new File(getFilesDir(), "porter_result.txt.tmp").delete();
        PorterDebug.init(this);
        PorterDebug.clear();
        PorterDebug.log("service: run start (replace=" + booleanExtra + ")");
        this.worker = new Thread(new Runnable() { // from class: org.nx2a.t0100152000022000.PorterService.2
            @Override // java.lang.Runnable
            public void run() {
                String stackTraceString;
                String str;
                try {
                    PorterService.this.runPorter(uri, uri2, uri3, booleanExtra);
                    stackTraceString = null;
                    str = null;
                } catch (PorterBackend.PorterException e) {
                    String message = e.getMessage();
                    stackTraceString = null;
                    str = message;
                } catch (Throwable th) {
                    Log.e(PorterService.TAG, "porter failed", th);
                    String message2 = th.getMessage() != null ? th.getMessage() : th.toString();
                    stackTraceString = Log.getStackTraceString(th);
                    str = message2;
                }
                if (str != null && !str.toLowerCase().contains("cancel")) {
                    PorterService.this.failureLogPath = PorterService.this.writeFailureLog(str, stackTraceString);
                }
                String writeDebugLog = PorterService.this.writeDebugLog();
                PorterService.this.writeResultFile(str, PorterService.this.failureLogPath, writeDebugLog);
                PorterDebug.log("service: DONE error=" + str + " debug=" + writeDebugLog);
                PorterService.this.broadcast(null, 100, str == null ? "Done." : null, true, str, writeDebugLog);
                if (str == null) {
                    PorterService.this.notifyCompletion();
                    PorterService.this.stopForeground(2);
                }
                if (PorterService.this.wakeLock != null && PorterService.this.wakeLock.isHeld()) {
                    PorterService.this.wakeLock.release();
                    PorterService.this.wakeLock = null;
                }
                PorterService.this.running = false;
                PorterService.this.stopSelf();
            }
        });
        this.worker.start();
        return 2;
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.cancelled = true;
        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
            this.wakeLock = null;
        }
        super.onDestroy();
    }

    private Notification buildNotification(String str, String str2, int i) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Game porter", 2));
        }
        Notification.Builder ongoing = new Notification.Builder(this, CHANNEL_ID).setContentTitle(str).setContentText(str2).setSmallIcon(R.drawable.stat_sys_download).setOngoing(true);
        if (i >= 0) {
            ongoing.setProgress(100, i, false);
        }
        return ongoing.build();
    }

    private Notification buildCompletionNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel(DONE_CHANNEL_ID, "Porter complete", 3));
        }
        Intent intent = new Intent(this, (Class<?>) LauncherActivity.class);
        intent.setFlags(335544320);
        return new Notification.Builder(this, DONE_CHANNEL_ID).setContentTitle(Lang.t("notif_done")).setContentText(Lang.t("notif_tap")).setSmallIcon(R.drawable.stat_sys_download_done).setContentIntent(PendingIntent.getActivity(this, 0, intent, 201326592)).setAutoCancel(true).setOngoing(false).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCompletion() {
        ((NotificationManager) getSystemService("notification")).notify(NOTIF_ID, buildCompletionNotification());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyProgress(String str, int i, String str2) {
        int indexOf;
        int lastIndexOf;
        if (str != null && str.equalsIgnoreCase("TRANSLATE") && str2 != null && (indexOf = str2.indexOf(" rows")) > 0 && (lastIndexOf = str2.lastIndexOf(47, indexOf)) > 0) {
            int i2 = lastIndexOf;
            while (i2 > 0 && Character.isDigit(str2.charAt(i2 - 1))) {
                i2--;
            }
            int i3 = lastIndexOf + 1;
            int i4 = i3;
            while (i4 < indexOf && Character.isDigit(str2.charAt(i4))) {
                i4++;
            }
            try {
                int parseInt = Integer.parseInt(str2.substring(i2, lastIndexOf));
                int parseInt2 = Integer.parseInt(str2.substring(i3, i4));
                if (parseInt2 > 0 && parseInt >= 0 && parseInt <= parseInt2) {
                    i = (parseInt * 100) / parseInt2;
                }
            } catch (Exception e) {
            }
        }
        ((NotificationManager) getSystemService("notification")).notify(NOTIF_ID, buildNotification(Lang.t("install_nsp"), Lang.stage(str) + " " + i + "% - " + str2 + ((str == null || !(str.equalsIgnoreCase("SPLIT") || str.equalsIgnoreCase("PACKAGE"))) ? "" : " " + Lang.t("notif_wait")), overallPercent(str, i)));
    }

    private void broadcast(String str, int i, String str2, boolean z, String str3) {
        broadcast(str, i, str2, z, str3, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcast(String str, int i, String str2, boolean z, String str3, String str4) {
        if (!z && str != null) {
            this.logTrail.append(str).append(' ').append(i).append("% - ").append(str2 != null ? str2 : "").append('\n');
            this.logTrailLines++;
        }
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.putExtra(EXTRA_STAGE, str);
        intent.putExtra(EXTRA_PERCENT, i);
        intent.putExtra(EXTRA_MESSAGE, str2);
        intent.putExtra(EXTRA_DONE, z);
        if (str3 != null) {
            intent.putExtra(EXTRA_ERROR, str3);
        }
        if (z && this.failureLogPath != null) {
            intent.putExtra(EXTRA_LOG, this.failureLogPath);
        }
        if (z && str4 != null) {
            intent.putExtra(EXTRA_DEBUG, str4);
        }
        sendBroadcast(intent);
        notifyListeners(str, i, str2, z, str3, z ? this.failureLogPath : null, str4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeResultFile(String str, String str2, String str3) {
        try {
            File file = new File(getFilesDir(), "porter_result.txt.tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            StringBuilder sb = new StringBuilder();
            StringBuilder append = sb.append("error=");
            if (str == null) {
                str = "";
            }
            append.append(str).append('\n');
            if (str2 != null) {
                sb.append("log=").append(str2).append('\n');
            }
            if (str3 != null) {
                sb.append("debug=").append(str3).append('\n');
            }
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
            file.renameTo(new File(getFilesDir(), RESULT_FILE));
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String writeFailureLog(String str, String str2) {
        byte[] bytes;
        String str3 = "MKart8Porter-failure-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt";
        StringBuilder sb = new StringBuilder();
        sb.append("MKart8Porter failure log\n");
        sb.append("Date: ").append(new Date().toString()).append('\n');
        if (this.validatedTitleId != null) {
            sb.append("Title: ").append(this.validatedTitleId).append('\n');
        }
        sb.append("Progress trail:\n");
        String sb2 = this.logTrail.toString();
        if (this.logTrailLines > 2000) {
            String[] split = sb2.split("\n");
            sb.append("[... ").append(this.logTrailLines - 2000).append(" earlier lines omitted ...]\n");
            for (int length = split.length - 2000; length < split.length; length++) {
                sb.append(split[length]).append('\n');
            }
        } else {
            sb.append(sb2);
        }
        sb.append("Error: ").append(str).append('\n');
        if (str2 != null) {
            sb.append("Stack trace:\n").append(str2).append('\n');
        }
        try {
            bytes = sb.toString().getBytes("UTF-8");
        } catch (Exception e) {
            bytes = sb.toString().getBytes();
        }
        return writeDocument(str3, bytes);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String writeDebugLog() {
        try {
            File file = PorterDebug.file();
            if (file != null && file.exists()) {
                int length = (int) file.length();
                byte[] bArr = new byte[length];
                FileInputStream fileInputStream = new FileInputStream(file);
                int i = 0;
                while (i < length) {
                    int read = fileInputStream.read(bArr, i, length - i);
                    if (read <= 0) {
                        break;
                    }
                    i += read;
                }
                fileInputStream.close();
                return writeDocument("MKart8Porter-debug-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt", bArr);
            }
            return null;
        } catch (Exception e) {
            Log.w(TAG, "debug log write failed", e);
            return null;
        }
    }

    private String writeDocument(String str, byte[] bArr) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str);
            contentValues.put("mime_type", "text/plain");
            contentValues.put("relative_path", Environment.DIRECTORY_DOCUMENTS);
            Uri insert = getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
            if (insert == null) {
                throw new IOException("MediaStore insert returned null");
            }
            OutputStream openOutputStream = getContentResolver().openOutputStream(insert);
            if (openOutputStream == null) {
                throw new IOException("MediaStore openOutputStream returned null");
            }
            try {
                openOutputStream.write(bArr);
                openOutputStream.close();
                return "Documents/" + str;
            } catch (Throwable th) {
                openOutputStream.close();
                throw th;
            }
        } catch (Exception e) {
            Log.w(TAG, "Documents log write failed, falling back to files dir", e);
            try {
                File file = new File(getExternalFilesDir(null), "porter-logs");
                file.mkdirs();
                FileOutputStream fileOutputStream = new FileOutputStream(new File(file, str));
                try {
                    fileOutputStream.write(bArr);
                    fileOutputStream.close();
                    return "App files/porter-logs/" + str;
                } catch (Throwable th2) {
                    fileOutputStream.close();
                    throw th2;
                }
            } catch (Exception e2) {
                Log.e(TAG, "failure log write failed entirely", e2);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0073  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void runPorter(android.net.Uri r10, android.net.Uri r11, android.net.Uri r12, boolean r13) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 370
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: org.nx2a.t0100152000022000.PorterService.runPorter(android.net.Uri, android.net.Uri, android.net.Uri, boolean):void");
    }

    private void writeState(File file, String str) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(EXTRA_STAGE, str);
        File file2 = new File(this.workDir, "porter_state.json.tmp");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file2), "UTF-8");
        outputStreamWriter.write(jSONObject.toString());
        outputStreamWriter.close();
        file2.renameTo(file);
    }

    private void doPreflight(boolean z) throws Exception {
        long availableBytes = new StatFs(this.root.getAbsolutePath()).getAvailableBytes();
        if (availableBytes < REQUIRED_FREE) {
            throw new PorterBackend.PorterException("Need at least 25 GB free, have " + (availableBytes / 1073741824) + " GB.");
        }
        if (!z && new File(this.root, "RomFS").exists()) {
            throw new PorterBackend.PorterException("A data pack is already installed. Confirm replacement to continue.");
        }
        this.progressCb.onProgress("PREFLIGHT", 100, "Free space OK (" + (availableBytes / 1073741824) + " GB).");
    }

    private void doExtract(Uri uri, Uri uri2, Uri uri3) throws Exception {
        if (this.nativePorter == null) {
            throw new PorterBackend.PorterException("Native extractor backend not wired yet (next slice).");
        }
        if (uri == null || uri2 == null || uri3 == null) {
            throw new PorterBackend.PorterException("Pick a base NSP, an update NSP and prod.keys first.");
        }
        ContentResolver contentResolver = getContentResolver();
        ParcelFileDescriptor openFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
        ParcelFileDescriptor openFileDescriptor2 = contentResolver.openFileDescriptor(uri2, "r");
        if (openFileDescriptor == null || openFileDescriptor2 == null) {
            throw new PorterBackend.PorterException("Could not open the picked NSPs.");
        }
        PorterDebug.log("service: doExtract PFDs opened baseFd=" + openFileDescriptor.getFd() + " updateFd=" + openFileDescriptor2.getFd());
        try {
            String readText = readText(contentResolver.openInputStream(uri3));
            String validateTitles = this.nativePorter.validateTitles(openFileDescriptor.getFd(), openFileDescriptor2.getFd(), readText);
            PorterDebug.log("service: doExtract validateTitles=" + validateTitles);
            String[] split = validateTitles.split(":");
            if (split.length != 2) {
                throw new PorterBackend.PorterException("Bad validation result.");
            }
            this.validatedTitleId = split[0];
            this.validatedCnmtVersion = split[1];
            this.progressCb.onProgress("EXTRACT", 0, "Title " + this.validatedTitleId + " validated.");
            PorterDebug.log("service: doExtract extract start");
            this.nativePorter.extract(openFileDescriptor.getFd(), openFileDescriptor2.getFd(), readText, new File(this.workDir, "extract").getAbsolutePath(), this.progressCb);
            PorterDebug.log("service: doExtract extract done");
            try {
                openFileDescriptor.close();
            } catch (Exception e) {
            }
            try {
                openFileDescriptor2.close();
            } catch (Exception e2) {
            }
        } finally {
        }
    }

    private void runPython(String str) throws Exception {
        if (this.pythonBackend == null) {
            throw new PorterBackend.PorterException("Python backend not wired yet (next slice).");
        }
        HashMap hashMap = new HashMap();
        hashMap.put("work", this.workDir.getAbsolutePath());
        hashMap.put("extract", new File(this.workDir, "extract").getAbsolutePath());
        hashMap.put("out", new File(this.workDir, "final").getAbsolutePath());
        hashMap.put("checkpoint", new File(this.workDir, str + "_ckpt").getAbsolutePath());
        if (str.equals("package")) {
            hashMap.put("titleId", this.validatedTitleId);
            hashMap.put("cnmtVersion", this.validatedCnmtVersion);
            hashMap.put("name", "Mario Kart 8 Deluxe");
            hashMap.put("display", "3.0.5");
        }
        this.pythonBackend.runStage(str, hashMap, this.progressCb);
    }

    private void doSwap(File file) throws Exception {
        File file2 = new File(this.root, "files.prev");
        File file3 = new File(this.workDir, "swap_journal.txt");
        boolean exists = file3.exists();
        if (!exists && file2.exists()) {
            deleteRecursive(file2);
        }
        ArrayList arrayList = new ArrayList();
        if (exists) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file3));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                } else {
                    arrayList.add(readLine);
                }
            }
            bufferedReader.close();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file3, true);
        try {
            boolean exists2 = new File(this.root, "RomFS").exists();
            for (String str : PACK_ENTRIES) {
                File file4 = new File(this.root, str);
                File file5 = new File(file2, str);
                if (exists2 && file4.exists() && !arrayList.contains("prev:" + str)) {
                    journalLine(fileOutputStream, "prev:" + str);
                    file2.mkdirs();
                    if (!file4.renameTo(file5)) {
                        throw new PorterBackend.PorterException("Swap failed moving " + str + " aside.");
                    }
                }
                File file6 = new File(file, str);
                if (file6.exists() && !arrayList.contains("in:" + str)) {
                    journalLine(fileOutputStream, "in:" + str);
                    if (!file6.renameTo(file4)) {
                        throw new PorterBackend.PorterException("Swap failed installing " + str + ".");
                    }
                }
            }
        } finally {
            fileOutputStream.close();
        }
    }

    private void journalLine(FileOutputStream fileOutputStream, String str) throws Exception {
        fileOutputStream.write((str + "\n").getBytes("UTF-8"));
        fileOutputStream.flush();
    }

    private String readFile(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        while (true) {
            int read = fileInputStream.read(bArr);
            if (read < 0) {
                fileInputStream.close();
                return byteArrayOutputStream.toString("UTF-8");
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
    }

    private String readText(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            throw new PorterBackend.PorterException("Could not read the picked file.");
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine == null) {
                bufferedReader.close();
                return sb.toString();
            }
            sb.append(readLine).append('\n');
        }
    }

    private void deleteRecursive(File file) {
        File[] listFiles;
        if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
            for (File file2 : listFiles) {
                deleteRecursive(file2);
            }
        }
        file.delete();
    }
}
