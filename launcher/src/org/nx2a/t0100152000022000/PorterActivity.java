package org.nx2a.t0100152000022000;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.nx2a.t0100152000022000.PorterService;

/* loaded from: classes.dex */
public class PorterActivity extends Activity {
    private static final int REQ_BASE = 1;
    private static final int REQ_KEYS = 3;
    private static final int REQ_UPDATE = 2;
    private Uri baseUri;
    private TextView baseVal;
    private Button cancelBtn;
    private Uri keysUri;
    private TextView keysVal;
    private TextView percentView;
    private ProgressBar progressBar;
    private TextView progressView;
    private TextView spaceVal;
    private Button startBtn;
    private TextView statusView;
    private Uri updateUri;
    private TextView updateVal;
    private boolean running = false;
    private boolean cancelling = false;
    private final PorterService.ProgressListener progressListener = new PorterService.ProgressListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.1
        @Override // org.nx2a.t0100152000022000.PorterService.ProgressListener
        public void onProgress(String str, int i, String str2, boolean z, String str3, String str4, String str5) {
            PorterDebug.log("activity: listener " + str + " " + i + "% " + ((str2 == null || str2.length() <= 80) ? str2 : str2.substring(0, 80)) + " done=" + z + " error=" + str3);
            if (!z || str3 == null) {
                PorterActivity.this.progressBar.setProgress(i);
                PorterActivity.this.percentView.setText(i + "%");
            }
            if (str2 != null) {
                if (str != null && (str.equalsIgnoreCase("SPLIT") || str.equalsIgnoreCase("PACKAGE"))) {
                    str2 = str2 + " " + Lang.t("notif_wait");
                }
                TextView textView = PorterActivity.this.progressView;
                if (str != null) {
                    str2 = Lang.stage(str) + ": " + str2;
                }
                textView.setText(str2);
            }
            if (z) {
                PorterActivity.this.running = false;
                PorterActivity.this.cancelling = false;
                PorterActivity.this.refreshButtons();
                if (str3 != null && str3.toLowerCase().contains("cancel")) {
                    PorterActivity.this.statusView.setText("Cancelled.");
                } else if (str3 == null) {
                    PorterActivity.this.statusView.setText("Done. The new data pack is installed - tap Back, then PLAY.");
                } else {
                    PorterActivity.this.statusView.setText("Porter failed: " + str3 + (str4 != null ? "\nLog: " + str4 : ""));
                }
                if (str5 != null) {
                    PorterActivity.this.statusView.setText(((Object) PorterActivity.this.statusView.getText()) + "\nDebug: " + str5);
                }
            }
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = getFilesDir();
        }
        Lang.loadFromEnv(this, externalFilesDir);
        requestWindowFeature(REQ_BASE);
        getWindow().setFlags(1024, 1024);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(REQ_BASE);
        linearLayout.setPadding(dp(24), dp(16), dp(24), dp(24));
        linearLayout.setBackgroundColor(-16040609);
        TextView textView = new TextView(this);
        textView.setText(Lang.t("install_nsp"));
        textView.setTextSize(26.0f);
        textView.setTextColor(-1);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(Lang.t("porter_sub"));
        textView2.setTextSize(14.0f);
        textView2.setTextColor(-4201481);
        textView2.setGravity(17);
        textView2.setPadding(0, dp(4), 0, dp(16));
        linearLayout.addView(textView2);
        this.baseVal = new TextView(this);
        linearLayout.addView(pickRow("Base NSP", this.baseVal, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PorterActivity.this.pick(PorterActivity.REQ_BASE);
            }
        }));
        this.updateVal = new TextView(this);
        linearLayout.addView(pickRow("Update NSP", this.updateVal, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PorterActivity.this.pick(PorterActivity.REQ_UPDATE);
            }
        }));
        this.keysVal = new TextView(this);
        linearLayout.addView(pickRow("prod.keys", this.keysVal, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PorterActivity.this.pick(PorterActivity.REQ_KEYS);
            }
        }));
        this.spaceVal = new TextView(this);
        this.spaceVal.setTextSize(14.0f);
        this.spaceVal.setTextColor(-1);
        this.spaceVal.setPadding(0, dp(8), 0, dp(8));
        linearLayout.addView(this.spaceVal);
        this.statusView = new TextView(this);
        this.statusView.setTextSize(14.0f);
        this.statusView.setTextColor(-1);
        linearLayout.addView(this.statusView);
        this.percentView = new TextView(this);
        this.percentView.setText("0%");
        this.percentView.setTextSize(28.0f);
        this.percentView.setTextColor(-1);
        this.percentView.setTypeface(Typeface.DEFAULT_BOLD);
        this.percentView.setGravity(17);
        linearLayout.addView(this.percentView);
        this.progressBar = new ProgressBar(this, null, R.attr.progressBarStyleHorizontal);
        this.progressBar.setMax(100);
        this.progressBar.setProgressTintList(ColorStateList.valueOf(-11713));
        this.progressBar.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(18)));
        linearLayout.addView(this.progressBar);
        this.progressView = new TextView(this);
        this.progressView.setTextSize(13.0f);
        this.progressView.setTextColor(-2039584);
        this.progressView.setPadding(0, dp(6), 0, dp(12));
        linearLayout.addView(this.progressView);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        this.startBtn = new Button(this);
        this.startBtn.setText(Lang.t("porter_start"));
        this.startBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.startBtn.setOnClickListener(new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PorterActivity.this.onStartPort();
            }
        });
        linearLayout2.addView(this.startBtn);
        this.cancelBtn = new Button(this);
        this.cancelBtn.setText(Lang.t("porter_cancel"));
        this.cancelBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.cancelBtn.setOnClickListener(new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Intent intent = new Intent(PorterActivity.this, (Class<?>) PorterService.class);
                intent.setAction(PorterService.ACTION_CANCEL);
                PorterActivity.this.startService(intent);
                PorterActivity.this.cancelling = true;
                PorterDebug.log("activity: Cancel tapped (ACTION_CANCEL sent)");
                PorterActivity.this.statusView.setText("Cancelling... waiting for the current stage to stop.");
                PorterActivity.this.cancelBtn.setEnabled(false);
            }
        });
        linearLayout2.addView(this.cancelBtn);
        linearLayout.addView(linearLayout2);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
        refreshAll();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        PorterDebug.init(this);
        PorterDebug.log("activity: onResume add listener");
        PorterService.addListener(this.progressListener);
        refreshAll();
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        PorterDebug.log("activity: onPause remove listener");
        PorterService.removeListener(this.progressListener);
    }

    private int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private LinearLayout pickRow(String str, TextView textView, View.OnClickListener onClickListener) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(REQ_BASE);
        linearLayout.setPadding(0, 0, 0, dp(12));
        Button button = new Button(this);
        button.setText(str);
        button.setOnClickListener(onClickListener);
        linearLayout.addView(button);
        textView.setTextSize(14.0f);
        textView.setTextColor(-1);
        textView.setText("Not picked yet.");
        linearLayout.addView(textView);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pick(int i) {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        startActivityForResult(intent, i);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        Uri data = intent.getData();
        try {
            getContentResolver().takePersistableUriPermission(data, REQ_BASE);
        } catch (Exception e) {
        }
        String displayName = displayName(data);
        if (i == REQ_BASE) {
            this.baseUri = data;
            this.baseVal.setText(displayName);
        } else if (i == REQ_UPDATE) {
            this.updateUri = data;
            this.updateVal.setText(displayName);
        } else if (i == REQ_KEYS) {
            this.keysUri = data;
            this.keysVal.setText(displayName);
        }
        refreshButtons();
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x002a, code lost:
    
        if (r6 != null) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0042, code lost:
    
        return r1.getLastPathSegment();
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x003b, code lost:
    
        r6.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0039, code lost:
    
        if (r6 == null) goto L27;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.lang.String displayName(android.net.Uri r9) {
        /*
            r8 = this;
            android.content.ContentResolver r0 = r8.getContentResolver()
            r1 = 1
            r6 = 0
            java.lang.String[] r2 = new java.lang.String[r1]     // Catch: java.lang.Throwable -> L2f java.lang.Exception -> L37
            java.lang.String r1 = "_display_name"
            r7 = 0
            r2[r7] = r1     // Catch: java.lang.Throwable -> L2f java.lang.Exception -> L37
            r4 = 0
            r5 = 0
            r3 = 0
            r1 = r9
            android.database.Cursor r6 = r0.query(r1, r2, r3, r4, r5)     // Catch: java.lang.Exception -> L2d java.lang.Throwable -> L2f
            if (r6 == 0) goto L2a
            boolean r9 = r6.moveToFirst()     // Catch: java.lang.Exception -> L2d java.lang.Throwable -> L2f
            if (r9 == 0) goto L2a
            java.lang.String r9 = r6.getString(r7)     // Catch: java.lang.Exception -> L2d java.lang.Throwable -> L2f
            if (r9 == 0) goto L2a
            if (r6 == 0) goto L29
            r6.close()
        L29:
            return r9
        L2a:
            if (r6 == 0) goto L3e
            goto L3b
        L2d:
            r0 = move-exception
            goto L39
        L2f:
            r0 = move-exception
            r9 = r0
            if (r6 == 0) goto L36
            r6.close()
        L36:
            throw r9
        L37:
            r0 = move-exception
            r1 = r9
        L39:
            if (r6 == 0) goto L3e
        L3b:
            r6.close()
        L3e:
            java.lang.String r9 = r1.getLastPathSegment()
            return r9
        */
        throw new UnsupportedOperationException("Method not decompiled: org.nx2a.t0100152000022000.PorterActivity.displayName(android.net.Uri):java.lang.String");
    }

    private File filesRoot() {
        File externalFilesDir = getExternalFilesDir(null);
        return externalFilesDir != null ? externalFilesDir : getFilesDir();
    }

    private void refreshAll() {
        String str;
        this.spaceVal.setText("Free space: " + (new StatFs(filesRoot().getAbsolutePath()).getAvailableBytes() / 1073741824) + " GB (needs 25 GB).");
        if (!this.running) {
            if (new File(getFilesDir(), PorterService.RESULT_FILE).exists()) {
                applyResultFileIfPresent();
            } else {
                boolean exists = new File(filesRoot(), "RomFS").exists();
                TextView textView = this.statusView;
                if (exists) {
                    str = Lang.t("porter_replace");
                } else {
                    str = "No data pack installed yet.";
                }
                textView.setText(str);
            }
        } else {
            applyResultFileIfPresent();
        }
        refreshButtons();
    }

    private void applyResultFileIfPresent() {
        String str;
        String str2;
        File file = new File(getFilesDir(), PorterService.RESULT_FILE);
        if (file.exists()) {
            String str3 = null;
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                str = null;
                str2 = null;
                while (true) {
                    try {
                        String readLine = bufferedReader.readLine();
                        if (readLine == null) {
                            break;
                        }
                        if (readLine.startsWith("error=")) {
                            str3 = readLine.substring(6);
                        } else if (readLine.startsWith("log=")) {
                            str = readLine.substring(4);
                        } else if (readLine.startsWith("debug=")) {
                            str2 = readLine.substring(6);
                        }
                    } catch (Exception e) {
                    }
                }
                bufferedReader.close();
            } catch (Exception e2) {
                str = null;
                str2 = null;
            }
            this.running = false;
            this.cancelling = false;
            if (str3 != null && !str3.isEmpty() && str3.toLowerCase().contains("cancel")) {
                this.statusView.setText("Cancelled.");
                return;
            }
            if (str3 != null && !str3.isEmpty()) {
                TextView textView = this.statusView;
                String str4 = "";
                StringBuilder append = new StringBuilder().append("Porter failed: ").append(str3).append((str == null || str.isEmpty()) ? "" : "\nLog: " + str);
                if (str2 != null && !str2.isEmpty()) {
                    str4 = "\nDebug: " + str2;
                }
                textView.setText(append.append(str4).toString());
                return;
            }
            this.statusView.setText("Done. The new data pack is installed - tap Back, then PLAY.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshButtons() {
        this.startBtn.setEnabled(!this.running && (this.baseUri != null && this.updateUri != null && this.keysUri != null));
        this.cancelBtn.setEnabled(this.running && !this.cancelling);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStartPort() {
        if (this.baseUri == null || this.updateUri == null || this.keysUri == null) {
            Toast.makeText(this, "Pick all three files first.", 0).show();
        } else if (new File(filesRoot(), "RomFS").exists()) {
            new AlertDialog.Builder(this).setTitle("Replace data pack?").setMessage("The current pack will be moved to files.prev/ (one backup kept). The port takes hours and needs 25 GB free.").setPositiveButton("Replace", new DialogInterface.OnClickListener() { // from class: org.nx2a.t0100152000022000.PorterActivity.7
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    PorterActivity.this.startPorter(true);
                }
            }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
        } else {
            startPorter(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startPorter(boolean z) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1347375700);
        }
        Intent intent = new Intent(this, (Class<?>) PorterService.class);
        intent.putExtra(PorterService.EXTRA_BASE_URI, this.baseUri);
        intent.putExtra(PorterService.EXTRA_UPDATE_URI, this.updateUri);
        intent.putExtra(PorterService.EXTRA_KEYS_URI, this.keysUri);
        intent.putExtra(PorterService.EXTRA_REPLACE, z);
        startService(intent);
        this.running = true;
        refreshButtons();
        this.statusView.setText("Porter running... keep this phone awake and charging.");
    }
}
