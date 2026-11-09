package org.nx2a.t0100152000022000;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NativeActivity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.nx2a.t0100152000022000.Lang;

/* loaded from: classes.dex */
public class LauncherActivity extends Activity {
    private static final int REQ_DATA_PICK = 1001;
    private static final int REQ_DRIVER_PICK = 1002;
    private static final int REQ_LOG_DIR = 1003;
    private static final long RETRY_DELAY_MS = 5000;
    private static final long RETRY_WINDOW_MS = 180000;
    private boolean dataOk;
    private Typeface displayFont;
    private boolean docked;
    private TextView driverVal;
    private TextView importDriverVal;
    private TextView importVal;
    private volatile boolean importing;
    private TextView modeVal;
    private LinearLayout playRow;
    private int present;
    private TextView presentVal;
    private ProgressBar progressBar;
    private TextView progressView;
    private TextView resVal;
    private File root;
    private TextView statusView;
    private TextView tiltVal;
    private boolean res720p = false;
    private boolean tilt = false;
    private String gameVer = "";
    private String zipName = "";
    private String driverZipName = "";
    private ArrayList<String> driverNames = new ArrayList<>();
    private ArrayList<String> driverSonames = new ArrayList<>();
    private ArrayList<Integer> driverSoCounts = new ArrayList<>();
    private int driverIndex = 0;
    private String envDriverName = "";
    private String langCode = "en-US";
    private boolean firstResume = true;
    private boolean resumed = false;

    private void applyFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    private BitmapDrawable darkenedBackground() {
        Bitmap bitmap;
        try {
            InputStream open = getAssets().open("launcher_bg.png");
            bitmap = BitmapFactory.decodeStream(open);
            try {
                open.close();
            } catch (Exception e) {
            }
        } catch (Exception e2) {
            bitmap = null;
        }
        if (bitmap == null) {
            try {
                InputStream open2 = getAssets().open("launcher_bg.jpg");
                bitmap = BitmapFactory.decodeStream(open2);
                open2.close();
            } catch (Exception e3) {
                return null;
            }
        }
        if (bitmap == null) {
            return null;
        }
        int i = getResources().getDisplayMetrics().widthPixels;
        int i2 = getResources().getDisplayMetrics().heightPixels;
        if (i <= 0 || i2 <= 0) {
            i = bitmap.getWidth();
            i2 = bitmap.getHeight();
        }
        Bitmap createBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        float f = i;
        float f2 = i2;
        float max = Math.max(f / bitmap.getWidth(), f2 / bitmap.getHeight());
        float width = bitmap.getWidth() * max;
        float height = bitmap.getHeight() * max;
        canvas.drawBitmap(bitmap, (Rect) null, new RectF((f - width) / 2.0f, (f2 - height) / 2.0f, (width + f) / 2.0f, (height + f2) / 2.0f), (Paint) null);
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0.0f, 0.0f, f * 0.65f, 0.0f, -654311424, 0, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, f, f2, paint);
        Paint paint2 = new Paint();
        paint2.setShader(new LinearGradient(0.0f, f2 * 0.65f, 0.0f, f2, 0, Integer.MIN_VALUE, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, f, f2, paint2);
        return new BitmapDrawable(getResources(), createBitmap);
    }

    private Typeface loadDisplayFont() {
        try {
            return Typeface.createFromAsset(getAssets(), "Bungee.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    private int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private GradientDrawable bg(int i, int i2, int i3, int i4) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(i2));
        if (i4 > 0) {
            gradientDrawable.setStroke(dp(i4), i3);
        }
        return gradientDrawable;
    }

    private BitmapDrawable checkerDrawable(int i, int i2, int i3) {
        int dp = dp(i3);
        int i4 = dp * 2;
        Bitmap createBitmap = Bitmap.createBitmap(i4, i4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = new Paint();
        paint.setColor(i);
        float f = i4;
        canvas.drawRect(0.0f, 0.0f, f, f, paint);
        paint.setColor(i2);
        float f2 = dp;
        canvas.drawRect(0.0f, 0.0f, f2, f2, paint);
        canvas.drawRect(f2, f2, f, f, paint);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), createBitmap);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        return bitmapDrawable;
    }

    private BitmapDrawable diagonalDrawable() {
        int dp = dp(200);
        Bitmap createBitmap = Bitmap.createBitmap(dp, dp, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = new Paint();
        paint.setColor(-1);
        float f = dp;
        canvas.drawRect(0.0f, 0.0f, f, f, paint);
        paint.setColor(-4200710);
        paint.setStrokeWidth(dp(14));
        int i = -dp;
        while (i < dp * 2) {
            float f2 = f;
            canvas.drawLine(i, f2, i + dp, 0.0f, paint);
            f = f2;
            i += dp(48);
        }
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), createBitmap);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        return bitmapDrawable;
    }

    private TextView label(String str, int i, int i2, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        if (z) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private LinearLayout menuRow(String str, TextView textView, boolean z, View.OnClickListener onClickListener) {
        GradientDrawable bg;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        if (!z) {
            bg = bg(-419430401, 14, -6631184, 1);
        } else {
            bg = bg(-14336, 18, -4683264, 2);
        }
        linearLayout.setBackground(bg);
        linearLayout.setElevation(dp(z ? 8 : 4));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(z ? 72 : 56));
        layoutParams.setMargins(0, 0, 0, dp(12));
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setClickable(true);
        linearLayout.setOnClickListener(onClickListener);
        TextView label = label(z ? "▶" : "❯", z ? 22 : 20, z ? -16777216 : -16031835, true);
        label.setPadding(dp(14), 0, dp(8), 0);
        linearLayout.addView(label);
        TextView label2 = label(str, z ? 22 : 17, -16777216, z);
        if (z && this.displayFont != null) {
            label2.setTypeface(this.displayFont);
        }
        label2.setShadowLayer(dp(1), dp(1), dp(1), 1073741824);
        label2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(label2);
        if (textView != null) {
            textView.setTextColor(-16031835);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextSize(15.0f);
            textView.setPadding(dp(8), 0, dp(8), 0);
            linearLayout.addView(textView);
        }
        TextView label3 = label(">", 20, z ? -16777216 : -16031835, true);
        label3.setPadding(0, 0, dp(14), 0);
        linearLayout.addView(label3);
        return linearLayout;
    }

    private LinearLayout checkerStrip(int i, int i2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(i)));
        for (int i3 = 0; i3 < i2; i3++) {
            View view = new View(this);
            view.setBackgroundColor(i3 % 2 == 0 ? -16777216 : -1);
            view.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
            linearLayout.addView(view);
        }
        return linearLayout;
    }

    private String driverName() {
        return this.driverIndex == 0 ? "System" : this.driverNames.get(this.driverIndex - 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshButtons() {
        this.playRow.setAlpha((!this.dataOk || this.importing) ? 0.5f : 1.0f);
        this.modeVal.setText(Lang.t(this.docked ? "mode_docked" : "mode_handheld"));
        this.presentVal.setText(Lang.t(this.present == 2 ? "aspect_stretch" : this.present == 1 ? "aspect_fill" : "aspect_fit"));
        this.resVal.setText(this.res720p ? "720p" : Lang.t("res_native"));
        this.driverVal.setText(driverRowLabel());
        this.driverVal.setTextSize(12.0f);
        this.tiltVal.setText(Lang.t(this.tilt ? "on" : "off"));
        this.importDriverVal.setText(Lang.t("driver_choose"));
        if (this.dataOk) {
            this.statusView.setText(Lang.t("status_installed") + (this.gameVer.length() > 0 ? " - " + this.gameVer : "") + ". " + Lang.t("tap_play"));
            this.statusView.setTextColor(-8586086);
        } else {
            this.statusView.setText(Lang.t("status_missing"));
            this.statusView.setTextColor(-30070);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dataCheck() {
        String str;
        int indexOf;
        this.dataOk = new File(this.root, "RomFS").isDirectory() && new File(this.root, "shaders.dmsv").isFile() && new File(this.root, "mods").isDirectory() && new File(this.root, "nx2a-data.json").isFile();
        this.gameVer = "";
        File file = new File(this.root, "nx2a-data.json");
        if (file.isFile()) {
            try {
                byte[] bArr = new byte[8192];
                FileInputStream fileInputStream = new FileInputStream(file);
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                if (read > 0 && (indexOf = (str = new String(bArr, 0, read, "UTF-8")).indexOf("\"display\"")) >= 0) {
                    int indexOf2 = str.indexOf(34, str.indexOf(58, indexOf) + 1);
                    int indexOf3 = indexOf2 >= 0 ? str.indexOf(34, indexOf2 + 1) : -1;
                    if (indexOf2 >= 0 && indexOf3 > indexOf2) {
                        this.gameVer = str.substring(indexOf2 + 1, indexOf3);
                    }
                }
            } catch (Exception e) {
            }
        }
        this.zipName = "";
        this.driverZipName = "";
        File[] listFiles = this.root.listFiles();
        if (listFiles != null) {
            for (File file2 : listFiles) {
                if (file2.isFile()) {
                    String lowerCase = file2.getName().toLowerCase();
                    if (lowerCase.endsWith(".zip")) {
                        if (lowerCase.contains("driver") || lowerCase.contains("turnip") || lowerCase.contains("vulkan")) {
                            if (this.driverZipName.length() == 0) {
                                this.driverZipName = file2.getName();
                            }
                        } else if (this.zipName.length() == 0) {
                            this.zipName = file2.getName();
                        }
                    }
                }
            }
        }
        scanDrivers();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File driverRoot() {
        File file = new File(getFilesDir(), "drivers");
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

    private void migrateDrivers() {
        File[] listFiles;
        File file = new File(this.root, "drivers");
        File driverRoot = driverRoot();
        if (!file.isDirectory() || file.equals(driverRoot) || (listFiles = file.listFiles()) == null) {
            return;
        }
        for (File file2 : listFiles) {
            if (file2.isDirectory()) {
                File file3 = new File(driverRoot, file2.getName());
                if (!file3.isDirectory()) {
                    file3.mkdirs();
                }
                File[] listFiles2 = file2.listFiles();
                if (listFiles2 != null) {
                    for (File file4 : listFiles2) {
                        if (file4.isFile()) {
                            File file5 = new File(file3, file4.getName());
                            if (!file5.isFile()) {
                                try {
                                    FileInputStream fileInputStream = new FileInputStream(file4);
                                    FileOutputStream fileOutputStream = new FileOutputStream(file5);
                                    byte[] bArr = new byte[65536];
                                    while (true) {
                                        int read = fileInputStream.read(bArr);
                                        if (read <= 0) {
                                            break;
                                        } else {
                                            fileOutputStream.write(bArr, 0, read);
                                        }
                                    }
                                    fileOutputStream.close();
                                    fileInputStream.close();
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanDrivers() {
        File[] listFiles;
        this.driverNames.clear();
        this.driverSonames.clear();
        this.driverSoCounts.clear();
        migrateDrivers();
        File driverRoot = driverRoot();
        File[] listFiles2 = driverRoot.isDirectory() ? driverRoot.listFiles() : null;
        if (listFiles2 != null) {
            for (File file : listFiles2) {
                if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
                    String str = "";
                    int i = 0;
                    for (File file2 : listFiles) {
                        if (file2.isFile() && file2.getName().toLowerCase().endsWith(".so")) {
                            if (i == 0) {
                                str = file2.getName();
                            }
                            i++;
                        }
                    }
                    if (i > 0) {
                        this.driverNames.add(file.getName());
                        this.driverSonames.add(str);
                        this.driverSoCounts.add(Integer.valueOf(i));
                    }
                }
            }
        }
        if (this.driverIndex > this.driverNames.size()) {
            this.driverIndex = 0;
        }
    }

    private String driverRowLabel() {
        if (this.driverIndex == 0) {
            return Lang.t("driver_system");
        }
        int i = this.driverIndex - 1;
        String str = this.driverNames.get(i) + " (" + this.driverSonames.get(i);
        if (this.driverSoCounts.get(i).intValue() > 1) {
            str = str + " +" + (this.driverSoCounts.get(i).intValue() - 1) + " more";
        }
        return str + ")";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String sonameFor(String str) {
        int indexOf = this.driverNames.indexOf(str);
        return indexOf >= 0 ? this.driverSonames.get(indexOf) : "";
    }

    private String readLang() {
        File file = new File(this.root, "env.txt");
        String str = "en-US";
        if (file.isFile()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bArr = new byte[8192];
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                if (read > 0) {
                    for (String str2 : new String(bArr, 0, read, "UTF-8").split("\n")) {
                        if (str2.startsWith("DM_LANG=")) {
                            str = str2.substring(8).trim();
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeEnv() {
        LauncherActivity launcherActivity;
        FileOutputStream fileOutputStream;
        StringBuilder sb;
        String str = this.langCode;
        StringBuilder sb2 = new StringBuilder();
        File file = new File(this.root, "env.txt");
        if (file.isFile()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bArr = new byte[8192];
                StringBuilder sb3 = new StringBuilder();
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    sb3.append(new String(bArr, 0, read, "UTF-8"));
                    fileInputStream = fileInputStream;
                }
                fileInputStream.close();
                for (String str2 : sb3.toString().split("\n")) {
                    if (!str2.startsWith("DM_VK_PRESENT_STRETCH=") && !str2.startsWith("DM_VK_PRESENT_FILL=") && !str2.startsWith("DM_VK_PRESENT_720P=") && !str2.startsWith("DM_VK_DRIVER=") && !str2.startsWith("DM_VK_DRIVER_DIR=") && !str2.startsWith("DM_VK_HOOK_LIB_DIR=") && str2.length() > 0) {
                        sb2.append(str2).append("\n");
                    }
                }
            } catch (Exception e) {
            }
        }
        try {
            fileOutputStream = new FileOutputStream(file);
            sb = new StringBuilder(sb2.toString());
            sb.append("DM_LANG=").append(str).append("\n");
            launcherActivity = this;
        } catch (Exception e2) {
            launcherActivity = this;
        }
        try {
            int i = 1;
            sb.append("DM_VK_PRESENT_STRETCH=").append(launcherActivity.present == 2 ? 1 : 0).append("\n");
            sb.append("DM_VK_PRESENT_FILL=").append(launcherActivity.present == 1 ? 1 : 0).append("\n");
            StringBuilder append = sb.append("DM_VK_PRESENT_720P=");
            if (!launcherActivity.res720p) {
                i = 0;
            }
            append.append(i).append("\n");
            sb.append("DM_VK_DRIVER=").append(launcherActivity.driverName()).append("\n");
            if (launcherActivity.driverIndex > 0) {
                sb.append("DM_VK_DRIVER_DIR=").append(new File(launcherActivity.driverRoot(), launcherActivity.driverName()).getAbsolutePath()).append("\n");
                sb.append("DM_VK_HOOK_LIB_DIR=").append(launcherActivity.getApplicationInfo().nativeLibraryDir).append("\n");
            }
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e3) {
            Toast.makeText(launcherActivity, "Cannot write env.txt", 0).show();
        }
    }

    private void loadSettings() {
        this.docked = false;
        this.tilt = false;
        File file = new File(this.root, "settings.txt");
        if (file.isFile()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bArr = new byte[4096];
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                if (read > 0) {
                    String[] split = new String(bArr, 0, read, "UTF-8").split("\n");
                    for (String str : split) {
                        if (str.startsWith("docked=")) {
                            this.docked = str.substring(7).trim().equals("1");
                        }
                        if (str.startsWith("tilt=")) {
                            this.tilt = str.substring(5).trim().equals("1");
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        this.present = 2;
        this.res720p = false;
        this.driverIndex = 0;
        File file2 = new File(this.root, "env.txt");
        if (file2.isFile()) {
            try {
                FileInputStream fileInputStream2 = new FileInputStream(file2);
                byte[] bArr2 = new byte[8192];
                int read2 = fileInputStream2.read(bArr2);
                fileInputStream2.close();
                if (read2 > 0) {
                    String str2 = new String(bArr2, 0, read2, "UTF-8");
                    boolean z = false;
                    for (String str3 : str2.split("\n")) {
                        if (str3.startsWith("DM_VK_PRESENT_STRETCH=1")) {
                            this.present = 2;
                            z = true;
                        } else if (str3.startsWith("DM_VK_PRESENT_FILL=1")) {
                            this.present = 1;
                            z = true;
                        } else if (str3.startsWith("DM_VK_PRESENT_720P=1")) {
                            this.res720p = true;
                        } else if (str3.startsWith("DM_VK_PRESENT_720P=0")) {
                            this.res720p = false;
                        } else if (str3.startsWith("DM_VK_DRIVER=")) {
                            String trim = str3.substring(13).trim();
                            if (!trim.equals("System") && !trim.equals("system") && trim.length() > 0) {
                                this.envDriverName = trim;
                            }
                        }
                    }
                    if (z && this.present != 2 && this.present != 1) {
                        this.present = 0;
                    }
                    if (str2.contains("DM_VK_PRESENT_STRETCH=0") && str2.contains("DM_VK_PRESENT_FILL=0")) {
                        this.present = 0;
                    }
                }
            } catch (Exception e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeSettings() {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        String str6;
        String str7;
        String str8;
        String str9;
        String str10;
        String str11;
        File file = new File(this.root, "settings.txt");
        int i = 0;
        String str12 = "1";
        if (!file.isFile()) {
            str10 = "UTF-8";
            str7 = null;
            str11 = null;
            str8 = null;
            str9 = null;
            str = null;
            str6 = "1";
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bArr = new byte[4096];
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                if (read <= 0) {
                    str = null;
                    str2 = null;
                    str3 = null;
                    str4 = null;
                    str5 = null;
                    str6 = "1";
                } else {
                    String[] split = new String(bArr, 0, read, "UTF-8").split("\n");
                    int length = split.length;
                    str = null;
                    str2 = null;
                    str3 = null;
                    str4 = null;
                    str5 = null;
                    str6 = "1";
                    while (i < length) {
                        String[] strArr = split;
                        try {
                            String str13 = strArr[i];
                            int i2 = i;
                            if (str13.startsWith("overlay=")) {
                                str12 = str13.substring(8).trim();
                            } else if (str13.startsWith("rumble=")) {
                                str6 = str13.substring(7).trim();
                            } else if (str13.startsWith("fps=")) {
                                str = str13.substring(4).trim();
                            } else if (str13.startsWith("controls=")) {
                                str2 = str13.substring(9).trim();
                            } else if (str13.startsWith("shadows=")) {
                                str3 = str13.substring(8).trim();
                            } else if (str13.startsWith("menu_x=")) {
                                str4 = str13.substring(7).trim();
                            } else if (str13.startsWith("menu_y=")) {
                                str5 = str13.substring(7).trim();
                            }
                            i = i2 + 1;
                            split = strArr;
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e2) {
                str = null;
                str2 = null;
                str3 = null;
                str4 = null;
                str5 = null;
                str6 = "1";
            }
            str7 = str2;
            str8 = str3;
            str9 = str4;
            str10 = "UTF-8";
            str11 = str5;
        }
        String str14 = str11;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            StringBuilder append = new StringBuilder().append("overlay=").append(str12).append("\nrumble=").append(str6).append("\ndocked=");
            int i3 = 1;
            StringBuilder append2 = append.append(this.docked ? 1 : 0).append("\ntilt=");
            if (!this.tilt) {
                i3 = 0;
            }
            fileOutputStream.write(append2.append(i3).append("\n").append(str != null ? "fps=" + str + "\n" : "").append(str7 != null ? "controls=" + str7 + "\n" : "").append(str8 != null ? "shadows=" + str8 + "\n" : "").append(str9 != null ? "menu_x=" + str9 + "\n" : "").append(str14 != null ? "menu_y=" + str14 + "\n" : "").toString().getBytes(str10));
            fileOutputStream.close();
        } catch (Exception e3) {
            Toast.makeText(this, "Cannot write settings.txt", 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String importZipTo(File file, File file2) {
        File file3;
        BufferedOutputStream bufferedOutputStream;
        BufferedInputStream bufferedInputStream;
        try {
            file3 = file;
            try {
                ZipFile zipFile = new ZipFile(file3);
                try {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    long j = 0;
                    long j2 = 0;
                    while (entries.hasMoreElements()) {
                        ZipEntry nextElement = entries.nextElement();
                        if (!nextElement.isDirectory() && nextElement.getSize() > 0) {
                            j2 += nextElement.getSize();
                        }
                    }
                    byte[] bArr = new byte[65536];
                    Enumeration<? extends ZipEntry> entries2 = zipFile.entries();
                    long j3 = 0;
                    long j4 = 0;
                    while (entries2.hasMoreElements()) {
                        ZipEntry nextElement2 = entries2.nextElement();
                        long j5 = j3;
                        String name = nextElement2.getName();
                        if (!name.startsWith("/") && !name.contains("../") && !name.equals("..")) {
                            File file4 = new File(file2, name);
                            if (nextElement2.isDirectory()) {
                                file4.mkdirs();
                                j3 = j5;
                            } else {
                                File parentFile = file4.getParentFile();
                                if (parentFile != null) {
                                    parentFile.mkdirs();
                                }
                                BufferedInputStream bufferedInputStream2 = new BufferedInputStream(zipFile.getInputStream(nextElement2));
                                long j6 = j;
                                BufferedOutputStream bufferedOutputStream2 = new BufferedOutputStream(new FileOutputStream(file4));
                                if (j2 > j6) {
                                    bufferedOutputStream = bufferedOutputStream2;
                                    bufferedInputStream = bufferedInputStream2;
                                    postImportProgress(name, j4, j4, j2);
                                    j5 = System.currentTimeMillis();
                                } else {
                                    bufferedOutputStream = bufferedOutputStream2;
                                    bufferedInputStream = bufferedInputStream2;
                                }
                                while (true) {
                                    int read = bufferedInputStream.read(bArr);
                                    if (read <= 0) {
                                        break;
                                    }
                                    bufferedOutputStream.write(bArr, 0, read);
                                    j4 += read;
                                    long currentTimeMillis = System.currentTimeMillis();
                                    if (j2 > j6 && currentTimeMillis - j5 >= 250) {
                                        postImportProgress(name, j4, j4, j2);
                                        j5 = currentTimeMillis;
                                    }
                                }
                                bufferedInputStream.close();
                                bufferedOutputStream.close();
                                if (j2 > j6) {
                                    postImportProgress(name, j4, j4, j2);
                                }
                                j3 = j5;
                                j = j6;
                            }
                        }
                        return "Refused unsafe path in zip: " + name;
                    }
                    try {
                        zipFile.close();
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                } catch (Exception e2) {
                    try {
                        zipFile.close();
                    } catch (Exception e3) {
                    }
                    return "Import failed: " + e2.getMessage();
                }
            } catch (Exception e4) {
                return "Cannot open " + file3.getName() + ".";
            }
        } catch (Exception e5) {
            file3 = file;
        }
    }

    private void launchPicker(int i) {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        intent.putExtra("android.intent.extra.MIME_TYPES", new String[]{"application/zip", "application/x-zip-compressed"});
        try {
            startActivityForResult(intent, i);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker available on this device.", 1).show();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:18:0x0029, code lost:
    
        if (r1 != null) goto L27;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x003d, code lost:
    
        r9 = r3.getLastPathSegment();
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0041, code lost:
    
        if (r9 == null) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0044, code lost:
    
        return "import.zip";
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:?, code lost:
    
        return r9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x003a, code lost:
    
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0038, code lost:
    
        if (r1 == null) goto L28;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.lang.String displayName(android.net.Uri r9) {
        /*
            r8 = this;
            r1 = 0
            android.content.ContentResolver r2 = r8.getContentResolver()     // Catch: java.lang.Throwable -> L2e java.lang.Exception -> L36
            r6 = 0
            r7 = 0
            r4 = 0
            r5 = 0
            r3 = r9
            android.database.Cursor r1 = r2.query(r3, r4, r5, r6, r7)     // Catch: java.lang.Exception -> L2c java.lang.Throwable -> L2e
            if (r1 == 0) goto L29
            boolean r9 = r1.moveToFirst()     // Catch: java.lang.Exception -> L2c java.lang.Throwable -> L2e
            if (r9 == 0) goto L29
            java.lang.String r9 = "_display_name"
            int r9 = r1.getColumnIndex(r9)     // Catch: java.lang.Exception -> L2c java.lang.Throwable -> L2e
            if (r9 < 0) goto L29
            java.lang.String r9 = r1.getString(r9)     // Catch: java.lang.Exception -> L2c java.lang.Throwable -> L2e
            if (r1 == 0) goto L28
            r1.close()
        L28:
            return r9
        L29:
            if (r1 == 0) goto L3d
            goto L3a
        L2c:
            r0 = move-exception
            goto L38
        L2e:
            r0 = move-exception
            r9 = r0
            if (r1 == 0) goto L35
            r1.close()
        L35:
            throw r9
        L36:
            r0 = move-exception
            r3 = r9
        L38:
            if (r1 == 0) goto L3d
        L3a:
            r1.close()
        L3d:
            java.lang.String r9 = r3.getLastPathSegment()
            if (r9 == 0) goto L44
            goto L46
        L44:
            java.lang.String r9 = "import.zip"
        L46:
            return r9
        */
        throw new UnsupportedOperationException("Method not decompiled: org.nx2a.t0100152000022000.LauncherActivity.displayName(android.net.Uri):java.lang.String");
    }

    private static class CountingInputStream extends FilterInputStream {
        long count;

        CountingInputStream(InputStream inputStream) {
            super(inputStream);
            this.count = 0L;
        }

        @Override // java.io.FilterInputStream, java.io.InputStream
        public int read() throws IOException {
            int read = super.read();
            if (read >= 0) {
                this.count++;
            }
            return read;
        }

        @Override // java.io.FilterInputStream, java.io.InputStream
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int read = super.read(bArr, i, i2);
            if (read > 0) {
                this.count += read;
            }
            return read;
        }
    }

    private static String formatGB(long j) {
        Locale locale = Locale.US;
        double d = j;
        Double.isNaN(d);
        return String.format(locale, "%.2f", Double.valueOf(d / 1.0E9d)).replace('.', ',');
    }

    private long docSize(Uri uri) {
        int columnIndex;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && (columnIndex = cursor.getColumnIndex("_size")) >= 0 && !cursor.isNull(columnIndex)) {
                long j = cursor.getLong(columnIndex);
                if (j > 0) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return j;
                }
            }
            if (cursor == null) {
                return -1L;
            }
        } catch (Exception e) {
            if (cursor == null) {
                return -1L;
            }
        } catch (Throwable th) {
            if (cursor == null) {
                throw th;
            }
            cursor.close();
            throw th;
        }
        cursor.close();
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postImportProgress(String str, long j, long j2, final long j3) {
        final int i;
        if (j3 > 0) {
            int i2 = (int) ((j * 100) / j3);
            if (i2 < 1) {
                i2 = 1;
            }
            if (i2 > 100) {
                i2 = 100;
            }
            i = i2;
        } else {
            i = 0;
        }
        final String str2 = "Importing: " + str + "\n" + i + "% (" + formatGB(j2) + "/" + formatGB(j3) + " GB)";
        runOnUiThread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.1
            @Override // java.lang.Runnable
            public void run() {
                if (j3 > 0) {
                    LauncherActivity.this.progressBar.setIndeterminate(false);
                    LauncherActivity.this.progressBar.setProgress(i);
                }
                LauncherActivity.this.progressView.setText(str2);
            }
        });
    }

    private void importFromUri(final Uri uri, final boolean z) {
        this.importing = true;
        refreshButtons();
        final String displayName = displayName(uri);
        final long docSize = docSize(uri);
        this.progressView.setText("Importing " + displayName + " ...");
        if (docSize > 0) {
            this.progressBar.setIndeterminate(false);
            this.progressBar.setProgress(0);
        } else {
            this.progressBar.setIndeterminate(true);
        }
        new Thread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.2
            @Override // java.lang.Runnable
            public void run() {
                String str;
                final String str2;
                InputStream openInputStream;
                String name;
                String str3;
                String str4;
                String substring = displayName.toLowerCase().endsWith(".zip") ? displayName.substring(0, displayName.length() - 4) : displayName;
                File file = z ? new File(LauncherActivity.this.driverRoot(), substring) : LauncherActivity.this.root;
                if (z) {
                    file.mkdirs();
                }
                try {
                    openInputStream = LauncherActivity.this.getContentResolver().openInputStream(uri);
                } catch (Exception e) {
                    str = "Import failed: " + e.getMessage();
                }
                if (openInputStream == null) {
                    throw new IOException("Cannot open " + displayName);
                }
                CountingInputStream countingInputStream = new CountingInputStream(new BufferedInputStream(openInputStream));
                ZipInputStream zipInputStream = new ZipInputStream(countingInputStream);
                byte[] bArr = new byte[65536];
                long j = 0;
                while (true) {
                    ZipEntry nextEntry = zipInputStream.getNextEntry();
                    if (nextEntry == null) {
                        str = null;
                        break;
                    }
                    name = nextEntry.getName();
                    if (name.startsWith("/") || name.contains("../")) {
                        break;
                    }
                    if (name.equals("..")) {
                        str3 = name;
                        break;
                    }
                    File file2 = new File(file, name);
                    if (nextEntry.isDirectory()) {
                        file2.mkdirs();
                    } else {
                        File parentFile = file2.getParentFile();
                        if (parentFile != null) {
                            parentFile.mkdirs();
                        }
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file2));
                        LauncherActivity.this.postImportProgress(name, countingInputStream.count, j, docSize);
                        String str5 = name;
                        long currentTimeMillis = System.currentTimeMillis();
                        while (true) {
                            int read = zipInputStream.read(bArr);
                            if (read <= 0) {
                                break;
                            }
                            bufferedOutputStream.write(bArr, 0, read);
                            long j2 = j + read;
                            long currentTimeMillis2 = System.currentTimeMillis();
                            if (docSize > 0 && currentTimeMillis2 - currentTimeMillis >= 250) {
                                LauncherActivity.this.postImportProgress(str5, countingInputStream.count, j2, docSize);
                                str4 = str5;
                                currentTimeMillis = currentTimeMillis2;
                            } else {
                                str4 = str5;
                            }
                            str5 = str4;
                            j = j2;
                        }
                        final String str6 = str5;
                        bufferedOutputStream.close();
                        if (docSize <= 0) {
                            LauncherActivity.this.runOnUiThread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.2.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    LauncherActivity.this.progressView.setText("Importing: " + str6);
                                }
                            });
                        }
                    }
                }
                str3 = name;
                str = "Refused unsafe path in zip: " + str3;
                zipInputStream.close();
                if (str != null) {
                    str2 = str;
                } else {
                    if (z) {
                        LauncherActivity.this.scanDrivers();
                        if (LauncherActivity.this.driverNames.contains(substring)) {
                            str2 = "Driver imported: " + substring + (LauncherActivity.this.sonameFor(substring).length() > 0 ? " (" + LauncherActivity.this.sonameFor(substring) + ")" : "") + ". Tap the driver row to select it, then Play.";
                        } else {
                            str2 = "Driver zip unpacked, but no .so was found inside it.";
                        }
                    } else {
                        LauncherActivity.this.dataCheck();
                        if (LauncherActivity.this.dataOk) {
                            str2 = "Import complete. Data pack installed" + (LauncherActivity.this.gameVer.length() > 0 ? " - " + LauncherActivity.this.gameVer : "") + ". Tap Play.";
                        } else {
                            str2 = "Import finished but the data pack is incomplete (RomFS / shaders / mods missing).";
                        }
                    }
                }
                final boolean z2 = str == null;
                LauncherActivity.this.runOnUiThread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.2.2
                    @Override // java.lang.Runnable
                    public void run() {
                        LauncherActivity.this.importing = false;
                        LauncherActivity.this.progressBar.setIndeterminate(false);
                        LauncherActivity.this.progressBar.setProgress(z2 ? 100 : 0);
                        LauncherActivity.this.progressView.setText(str2);
                        LauncherActivity.this.dataCheck();
                        LauncherActivity.this.refreshButtons();
                    }
                });
            }
        }).start();
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        if (i != REQ_DATA_PICK) {
            if (i != REQ_DRIVER_PICK) {
                if (i == REQ_LOG_DIR) {
                    Uri data = intent.getData();
                    try {
                        getContentResolver().takePersistableUriPermission(data, 3);
                        saveLogDirUri(data);
                        File file = new File(this.root, "log.txt");
                        if (file.isFile()) {
                            exportLogToTree(data, file);
                        } else {
                            Toast.makeText(this, Lang.t("export_none"), 0).show();
                        }
                        return;
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not save folder permission", 0).show();
                        return;
                    }
                }
                return;
            }
            importFromUri(intent.getData(), true);
            return;
        }
        importFromUri(intent.getData(), false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doImport() {
        launchPicker(REQ_DATA_PICK);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doImportDriver() {
        launchPicker(REQ_DRIVER_PICK);
    }

    private void doImportOld() {
        if (this.zipName.length() == 0) {
            Toast.makeText(this, "No zip in the files folder. Put the data pack zip there first.", 1).show();
            return;
        }
        this.importing = true;
        refreshButtons();
        this.progressView.setText("Importing " + this.zipName + " ...");
        this.progressBar.setProgress(0);
        new Thread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.3
            @Override // java.lang.Runnable
            public void run() {
                final String importZipTo = LauncherActivity.this.importZipTo(new File(LauncherActivity.this.root, LauncherActivity.this.zipName), LauncherActivity.this.root);
                if (importZipTo == null) {
                    LauncherActivity.this.dataCheck();
                    if (LauncherActivity.this.dataOk) {
                        importZipTo = "Import complete. Data pack installed" + (LauncherActivity.this.gameVer.length() > 0 ? " - " + LauncherActivity.this.gameVer : "") + ". Tap Play.";
                    } else {
                        importZipTo = "Import finished but the data pack is incomplete (RomFS / shaders / mods missing).";
                    }
                }
                LauncherActivity.this.runOnUiThread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.3.1
                    @Override // java.lang.Runnable
                    public void run() {
                        LauncherActivity.this.importing = false;
                        LauncherActivity.this.progressView.setText(importZipTo);
                        LauncherActivity.this.progressBar.setProgress(0);
                        LauncherActivity.this.dataCheck();
                        LauncherActivity.this.refreshButtons();
                    }
                });
            }
        }).start();
    }

    private void doImportDriverOld() {
        if (this.driverZipName.length() == 0) {
            Toast.makeText(this, "No driver zip in the files folder. Put a Turnip driver zip there (name it *driver* or *turnip*).", 1).show();
            return;
        }
        this.importing = true;
        refreshButtons();
        this.progressView.setText("Importing driver " + this.driverZipName + " ...");
        this.progressBar.setProgress(0);
        new Thread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.4
            @Override // java.lang.Runnable
            public void run() {
                String substring = LauncherActivity.this.driverZipName.substring(0, LauncherActivity.this.driverZipName.length() - 4);
                File file = new File(LauncherActivity.this.driverRoot(), substring);
                file.mkdirs();
                final String importZipTo = LauncherActivity.this.importZipTo(new File(LauncherActivity.this.root, LauncherActivity.this.driverZipName), file);
                if (importZipTo == null) {
                    LauncherActivity.this.scanDrivers();
                    if (LauncherActivity.this.driverNames.contains(substring)) {
                        importZipTo = "Driver imported: " + substring + (LauncherActivity.this.sonameFor(substring).length() > 0 ? " (" + LauncherActivity.this.sonameFor(substring) + ")" : "") + ". Tap the driver row to select it, then Play.";
                    } else {
                        importZipTo = "Driver zip unpacked, but no .so was found inside it.";
                    }
                }
                LauncherActivity.this.runOnUiThread(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.4.1
                    @Override // java.lang.Runnable
                    public void run() {
                        LauncherActivity.this.importing = false;
                        LauncherActivity.this.progressView.setText(importZipTo);
                        LauncherActivity.this.progressBar.setProgress(0);
                        LauncherActivity.this.dataCheck();
                        LauncherActivity.this.refreshButtons();
                    }
                });
            }
        }).start();
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(128);
        applyFullscreen();
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = getFilesDir();
        }
        this.root = externalFilesDir;
        maybeClearCacheOnUpdate();
        loadSettings();
        this.langCode = Lang.loadFromEnv(this, this.root);
        ensureBundledDriver();
        dataCheck();
        if (this.envDriverName.length() > 0) {
            int indexOf = this.driverNames.indexOf(this.envDriverName);
            this.driverIndex = indexOf >= 0 ? indexOf + 1 : 0;
        } else {
            this.driverIndex = 0;
        }
        applyBundledDriverDefault();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        Drawable darkenedBackground = darkenedBackground();
        if (darkenedBackground != null) {
            linearLayout.setBackground(darkenedBackground);
        } else {
            linearLayout.setBackground(checkerDrawable(-7352072, -4331525, 48));
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 11.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(24), dp(12), dp(16), dp(20));
        scrollView.addView(linearLayout2, new ViewGroup.LayoutParams(-1, -2));
        this.displayFont = loadDisplayFont();
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView label = label("MARIO KART", 34, -1, true);
        if (this.displayFont != null) {
            label.setTypeface(this.displayFont);
        }
        label.setShadowLayer(dp(4), dp(2), dp(2), -16777216);
        label.setGravity(17);
        TextView label2 = label("8  DELUXE", 26, -10496, true);
        if (this.displayFont != null) {
            label2.setTypeface(this.displayFont);
        }
        label2.setShadowLayer(dp(4), dp(2), dp(2), -16777216);
        label2.setGravity(17);
        linearLayout3.addView(label);
        linearLayout3.addView(label2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, 0, 0, dp(16));
        linearLayout3.setLayoutParams(layoutParams);
        linearLayout2.addView(linearLayout3);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(17);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.setMargins(0, 0, 0, dp(12));
        linearLayout4.setLayoutParams(layoutParams2);
        TextView label3 = label(Lang.t("language"), 17, -16777216, true);
        label3.setBackground(bg(-419430401, 14, -6631184, 1));
        label3.setPadding(dp(16), dp(8), dp(16), dp(8));
        linearLayout4.addView(label3);
        Button button = new Button(this);
        button.setText(currentLanguageName());
        button.setBackground(bg(-419430401, 14, -6631184, 1));
        button.setTextColor(-16031835);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(17.0f);
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setOnClickListener(new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.showLanguageDialog();
            }
        });
        linearLayout4.addView(button);
        linearLayout2.addView(linearLayout4);
        this.playRow = menuRow(Lang.t("play"), null, true, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (LauncherActivity.this.dataOk && !LauncherActivity.this.importing) {
                    LauncherActivity.this.writeEnv();
                    LauncherActivity.this.writeSettings();
                    LauncherActivity.this.startGame(true);
                    return;
                }
                Toast.makeText(LauncherActivity.this, Lang.t("play_missing"), 0).show();
            }
        });
        linearLayout2.addView(this.playRow);
        linearLayout2.addView(menuRow(Lang.t("install_nsp"), label("", 15, -16031835, true), false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.startActivity(new Intent(LauncherActivity.this, (Class<?>) PorterActivity.class));
            }
        }));
        linearLayout2.addView(menuRow(Lang.t("import_data"), null, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (LauncherActivity.this.importing) {
                    return;
                }
                LauncherActivity.this.doImport();
            }
        }));
        this.modeVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("mode"), this.modeVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.docked = !LauncherActivity.this.docked;
                LauncherActivity.this.writeSettings();
                LauncherActivity.this.refreshButtons();
            }
        }));
        this.tiltVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("tilt"), this.tiltVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.10
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.tilt = !LauncherActivity.this.tilt;
                LauncherActivity.this.writeSettings();
                LauncherActivity.this.refreshButtons();
            }
        }));
        this.presentVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("aspect"), this.presentVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.11
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.present = (LauncherActivity.this.present + 1) % 3;
                LauncherActivity.this.writeEnv();
                LauncherActivity.this.refreshButtons();
            }
        }));
        this.resVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("resolution"), this.resVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.12
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.res720p = !LauncherActivity.this.res720p;
                LauncherActivity.this.writeEnv();
                LauncherActivity.this.refreshButtons();
            }
        }));
        this.driverVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("driver"), this.driverVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.13
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (LauncherActivity.this.driverNames.isEmpty()) {
                    Toast.makeText(LauncherActivity.this, Lang.t("driver_none"), 0).show();
                    return;
                }
                LauncherActivity.this.driverIndex = (LauncherActivity.this.driverIndex + 1) % (LauncherActivity.this.driverNames.size() + 1);
                LauncherActivity.this.writeEnv();
                LauncherActivity.this.refreshButtons();
            }
        }));
        this.importDriverVal = label("", 15, -16031835, true);
        linearLayout2.addView(menuRow(Lang.t("import_driver"), this.importDriverVal, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.14
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (LauncherActivity.this.importing) {
                    return;
                }
                LauncherActivity.this.doImportDriver();
            }
        }));
        linearLayout2.addView(menuRow(Lang.t("export_log"), null, false, new View.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.15
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LauncherActivity.this.exportLog();
            }
        }));
        LinearLayout checkerStrip = checkerStrip(16, 24);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, dp(16));
        layoutParams3.setMargins(0, dp(8), 0, 0);
        checkerStrip.setLayoutParams(layoutParams3);
        checkerStrip.setElevation(dp(4));
        linearLayout2.addView(checkerStrip);
        linearLayout.addView(scrollView);
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(1);
        linearLayout5.setPadding(dp(24), dp(16), dp(24), dp(32));
        linearLayout5.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        TextView label4 = label("★  ★  ★", 20, -10496, true);
        label4.setShadowLayer(dp(3), dp(1), dp(1), -16777216);
        label4.setGravity(17);
        linearLayout5.addView(label4);
        TextView label5 = label("8", 110, -1703918, true);
        if (this.displayFont != null) {
            label5.setTypeface(this.displayFont);
        }
        label5.setShadowLayer(dp(6), dp(3), dp(3), -16777216);
        label5.setGravity(17);
        linearLayout5.addView(label5);
        TextView label6 = label("DELUXE", 30, -10496, true);
        if (this.displayFont != null) {
            label6.setTypeface(this.displayFont);
        }
        label6.setShadowLayer(dp(4), dp(2), dp(2), -16777216);
        label6.setGravity(17);
        linearLayout5.addView(label6);
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(1);
        linearLayout6.setBackground(bg(-872415232, 16, -10496, 1));
        linearLayout6.setElevation(dp(8));
        linearLayout6.setPadding(dp(18), dp(16), dp(18), dp(16));
        this.statusView = label("", 15, -1, false);
        this.statusView.setPadding(0, 0, 0, dp(12));
        linearLayout6.addView(this.statusView);
        this.progressBar = new ProgressBar(this, null, R.attr.progressBarStyleHorizontal);
        this.progressBar.setMax(100);
        this.progressBar.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(14)));
        linearLayout6.addView(this.progressBar);
        this.progressView = label("", 14, -2236963, false);
        this.progressView.setPadding(0, dp(6), 0, dp(8));
        linearLayout6.addView(this.progressView);
        linearLayout6.addView(label(Lang.t("foot"), 13, -4473925, false));
        linearLayout5.addView(linearLayout6);
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        linearLayout5.addView(view);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, dp(16));
        layoutParams4.setMargins(0, dp(16), 0, dp(8));
        LinearLayout checkerStrip2 = checkerStrip(16, 24);
        checkerStrip2.setLayoutParams(layoutParams4);
        checkerStrip2.setElevation(dp(4));
        linearLayout5.addView(checkerStrip2);
        ScrollView scrollView2 = new ScrollView(this);
        scrollView2.setFillViewport(true);
        scrollView2.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 9.0f));
        scrollView2.addView(linearLayout5);
        linearLayout.addView(scrollView2);
        refreshButtons();
        setContentView(linearLayout);
    }

    private void maybeClearCacheOnUpdate() {
        try {
            int i = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if (i < 0) {
                return;
            }
            File file = new File(getFilesDir(), "cache_version.txt");
            int i2 = -1;
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String readLine = bufferedReader.readLine();
                bufferedReader.close();
                if (readLine != null) {
                    i2 = Integer.parseInt(readLine.trim());
                }
            } catch (Exception e) {
            }
            if (i2 != i && this.root != null) {
                try {
                    File file2 = new File(this.root, "vk_pipeline_cache.bin");
                    if (file2.exists()) {
                        file2.delete();
                    }
                } catch (Exception e2) {
                }
            }
            try {
                FileWriter fileWriter = new FileWriter(file, false);
                fileWriter.write(Integer.toString(i));
                fileWriter.close();
            } catch (Exception e3) {
            }
        } catch (Exception e4) {
        }
    }

    private String currentLanguageName() {
        for (Lang.Entry entry : Lang.languages()) {
            if (entry.code.equals(this.langCode)) {
                return entry.name;
            }
        }
        return this.langCode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showLanguageDialog() {
        List<Lang.Entry> languages = Lang.languages();
        final String[] strArr = new String[languages.size()];
        String[] strArr2 = new String[languages.size()];
        int i = 0;
        for (int i2 = 0; i2 < languages.size(); i2++) {
            strArr[i2] = languages.get(i2).code;
            strArr2[i2] = languages.get(i2).name;
            if (strArr[i2].equals(this.langCode)) {
                i = i2;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Lang.t("language"));
        builder.setSingleChoiceItems(strArr2, i, new DialogInterface.OnClickListener() { // from class: org.nx2a.t0100152000022000.LauncherActivity.16
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i3) {
                LauncherActivity.this.langCode = strArr[i3];
                LauncherActivity.this.writeEnv();
                dialogInterface.dismiss();
                LauncherActivity.this.recreate();
            }
        });
        builder.setNegativeButton(Lang.t("porter_cancel"), (DialogInterface.OnClickListener) null);
        builder.show();
    }

    private void ensureBundledDriver() {
        boolean z;
        File[] listFiles;
        try {
            InputStream open = getAssets().open("libvulkan_freedreno.so");
            File file = new File(driverRoot(), "Turnip-Duo-v4.4-exp");
            if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
                for (File file2 : listFiles) {
                    if (file2.isFile() && file2.getName().toLowerCase().endsWith(".so")) {
                        z = true;
                        break;
                    }
                }
            }
            z = false;
            if (z) {
                open.close();
                return;
            }
            file.mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(new File(file, "libvulkan_freedreno.so"));
            byte[] bArr = new byte[65536];
            while (true) {
                int read = open.read(bArr);
                if (read <= 0) {
                    fileOutputStream.close();
                    open.close();
                    return;
                }
                fileOutputStream.write(bArr, 0, read);
            }
        } catch (Exception e) {
        }
    }

    private void applyBundledDriverDefault() {
        int indexOf;
        try {
            File file = new File(getFilesDir(), "driver_default_applied.txt");
            if (file.isFile()) {
                return;
            }
            if (this.envDriverName.length() == 0 && (indexOf = this.driverNames.indexOf("Turnip-Duo-v4.4-exp")) >= 0) {
                this.driverIndex = indexOf + 1;
                writeEnv();
            }
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write("1");
            fileWriter.close();
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exportLog() {
        if (this.root == null) {
            return;
        }
        File file = new File(this.root, "log.txt");
        Uri savedLogDirUri = savedLogDirUri();
        if (savedLogDirUri == null) {
            launchLogDirPicker();
        } else {
            exportLogToTree(savedLogDirUri, file);
        }
    }

    private void launchLogDirPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(195);
        try {
            intent.putExtra("android.provider.extra.INITIAL_URI", DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:"));
        } catch (Exception e) {
        }
        try {
            startActivityForResult(intent, REQ_LOG_DIR);
        } catch (Exception e2) {
            Toast.makeText(this, "No folder picker available on this device.", 1).show();
        }
    }

    private File logDirFile() {
        return new File(getFilesDir(), "logdir.txt");
    }

    private Uri savedLogDirUri() {
        try {
            File logDirFile = logDirFile();
            if (!logDirFile.isFile()) {
                return null;
            }
            byte[] bArr = new byte[2048];
            FileInputStream fileInputStream = new FileInputStream(logDirFile);
            int read = fileInputStream.read(bArr);
            fileInputStream.close();
            if (read <= 0) {
                return null;
            }
            String trim = new String(bArr, 0, read, "UTF-8").trim();
            if (trim.length() > 0) {
                return Uri.parse(trim);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveLogDirUri(Uri uri) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(logDirFile());
            fileOutputStream.write(uri.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e) {
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:29:0x005b, code lost:
    
        if (r11 == null) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:5:0x0047, code lost:
    
        if (r11 != null) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0049, code lost:
    
        r11.close();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private android.net.Uri findChildDoc(android.net.Uri r10, java.lang.String r11, java.lang.String r12) {
        /*
            r9 = this;
            r1 = 0
            android.net.Uri r3 = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(r10, r11)     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            android.content.ContentResolver r2 = r9.getContentResolver()     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            r11 = 2
            java.lang.String[] r4 = new java.lang.String[r11]     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            java.lang.String r11 = "document_id"
            r0 = 0
            r4[r0] = r11     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            java.lang.String r11 = "_display_name"
            r8 = 1
            r4[r8] = r11     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            r6 = 0
            r7 = 0
            r5 = 0
            android.database.Cursor r11 = r2.query(r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> L4f java.lang.Exception -> L59
            if (r11 == 0) goto L47
        L20:
            boolean r2 = r11.moveToNext()     // Catch: java.lang.Throwable -> L41 java.lang.Exception -> L45
            if (r2 == 0) goto L47
            java.lang.String r2 = r11.getString(r0)     // Catch: java.lang.Throwable -> L41 java.lang.Exception -> L45
            java.lang.String r3 = r11.getString(r8)     // Catch: java.lang.Throwable -> L41 java.lang.Exception -> L45
            boolean r3 = r12.equals(r3)     // Catch: java.lang.Throwable -> L41 java.lang.Exception -> L45
            if (r3 == 0) goto L40
            android.net.Uri r10 = android.provider.DocumentsContract.buildDocumentUriUsingTree(r10, r2)     // Catch: java.lang.Throwable -> L41 java.lang.Exception -> L45
            if (r11 == 0) goto L3f
            r11.close()     // Catch: java.lang.Exception -> L3e
            goto L3f
        L3e:
            r0 = move-exception
        L3f:
            return r10
        L40:
            goto L20
        L41:
            r0 = move-exception
            r10 = r0
            r1 = r11
            goto L51
        L45:
            r0 = move-exception
            goto L5b
        L47:
            if (r11 == 0) goto L5e
        L49:
            r11.close()     // Catch: java.lang.Exception -> L4d
            goto L5e
        L4d:
            r0 = move-exception
            goto L5e
        L4f:
            r0 = move-exception
            r10 = r0
        L51:
            if (r1 == 0) goto L58
            r1.close()     // Catch: java.lang.Exception -> L57
            goto L58
        L57:
            r0 = move-exception
        L58:
            throw r10
        L59:
            r0 = move-exception
            r11 = r1
        L5b:
            if (r11 == 0) goto L5e
            goto L49
        L5e:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: org.nx2a.t0100152000022000.LauncherActivity.findChildDoc(android.net.Uri, java.lang.String, java.lang.String):android.net.Uri");
    }

    private void exportLogToTree(Uri uri, File file) {
        try {
            if (!file.isFile()) {
                Toast.makeText(this, Lang.t("export_none"), 0).show();
                return;
            }
            ContentResolver contentResolver = getContentResolver();
            String treeDocumentId = DocumentsContract.getTreeDocumentId(uri);
            Uri buildDocumentUriUsingTree = DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId);
            Uri findChildDoc = findChildDoc(uri, treeDocumentId, "MKart8-logs");
            if (findChildDoc == null) {
                findChildDoc = DocumentsContract.createDocument(contentResolver, buildDocumentUriUsingTree, "vnd.android.document/directory", "MKart8-logs");
            }
            try {
                if (findChildDoc == null) {
                    throw new Exception("Could not create MKart8-logs");
                }
                String documentId = DocumentsContract.getDocumentId(findChildDoc);
                copyOneToTree(contentResolver, uri, documentId, "log.txt", file);
                File file2 = new File(this.root, "log.prev.txt");
                if (file2.isFile()) {
                    try {
                        copyOneToTree(contentResolver, uri, documentId, "log.prev.txt", file2);
                    } catch (Exception e) {
                    }
                }
                this.progressView.setText(Lang.t("export_done"));
                Toast.makeText(this, Lang.t("export_done"), 1).show();
            } catch (Exception e2) {
                Toast.makeText(this, "Could not export log", 0).show();
            }
        } catch (Exception e3) {
        }
    }

    private void copyOneToTree(ContentResolver contentResolver, Uri uri, String str, String str2, File file) throws Exception {
        Uri findChildDoc = findChildDoc(uri, str, str2);
        if (findChildDoc == null) {
            findChildDoc = DocumentsContract.createDocument(contentResolver, DocumentsContract.buildDocumentUriUsingTree(uri, str), "text/plain", str2);
        }
        if (findChildDoc == null) {
            throw new Exception("Could not create " + str2);
        }
        OutputStream openOutputStream = contentResolver.openOutputStream(findChildDoc, "wt");
        if (openOutputStream == null) {
            throw new Exception("Could not open " + str2);
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bArr = new byte[65536];
        while (true) {
            int read = fileInputStream.read(bArr);
            if (read <= 0) {
                openOutputStream.close();
                fileInputStream.close();
                return;
            }
            openOutputStream.write(bArr, 0, read);
        }
    }

    private void writeFlag(String str, String str2) {
        try {
            FileWriter fileWriter = new FileWriter(new File(this.root, str), false);
            if (str2 != null) {
                fileWriter.write(str2);
            }
            fileWriter.close();
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String readFlag(String str) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(this.root, str)));
            String readLine = bufferedReader.readLine();
            bufferedReader.close();
            return readLine;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteFlag(String str) {
        try {
            File file = new File(this.root, str);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startGame(boolean z) {
        if (this.root == null) {
            return;
        }
        writeFlag("game_start.txt", Long.toString(System.currentTimeMillis()));
        deleteFlag("clean_exit.txt");
        if (z) {
            deleteFlag("auto_retried.txt");
        } else {
            writeFlag("auto_retried.txt", "1");
        }
        startActivity(new Intent(this, (Class<?>) NativeActivity.class));
    }

    private void checkAutoRetry() {
        String readFlag;
        final long j;
        if (this.root == null || (readFlag = readFlag("game_start.txt")) == null) {
            return;
        }
        try {
            j = Long.parseLong(readFlag.trim());
        } catch (Exception e) {
            j = -1;
        }
        boolean exists = new File(this.root, "clean_exit.txt").exists();
        boolean z = j < 0 || System.currentTimeMillis() - j >= RETRY_WINDOW_MS;
        boolean exists2 = new File(this.root, "auto_retried.txt").exists();
        if (exists || z || exists2) {
            deleteFlag("game_start.txt");
            deleteFlag("auto_retried.txt");
            deleteFlag("clean_exit.txt");
            return;
        }
        new Handler().postDelayed(new Runnable() { // from class: org.nx2a.t0100152000022000.LauncherActivity.17
            @Override // java.lang.Runnable
            public void run() {
                String readFlag2;
                long j2;
                if (LauncherActivity.this.resumed && (readFlag2 = LauncherActivity.this.readFlag("game_start.txt")) != null) {
                    try {
                        j2 = Long.parseLong(readFlag2.trim());
                    } catch (Exception e2) {
                        j2 = -1;
                    }
                    if (j2 != j) {
                        return;
                    }
                    LauncherActivity.this.startGame(false);
                }
            }
        }, RETRY_DELAY_MS);
    }

    private void reloadTilt() {
        this.tilt = false;
        File file = new File(this.root, "settings.txt");
        if (file.isFile()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bArr = new byte[4096];
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                if (read > 0) {
                    for (String str : new String(bArr, 0, read, "UTF-8").split("\n")) {
                        if (str.startsWith("tilt=")) {
                            this.tilt = str.substring(5).trim().equals("1");
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.resumed = true;
        applyFullscreen();
        maybeClearCacheOnUpdate();
        boolean z = this.firstResume;
        this.firstResume = false;
        if (z) {
            deleteFlag("game_start.txt");
            deleteFlag("auto_retried.txt");
            deleteFlag("clean_exit.txt");
        } else {
            checkAutoRetry();
        }
        if (!this.importing) {
            reloadTilt();
            dataCheck();
            refreshButtons();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.resumed = false;
    }
}
