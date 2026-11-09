package org.nx2a.t0100152000022000;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class Lang {
    private static JSONObject strings = null;
    private static List<Entry> langs = new ArrayList();
    private static String current = "en-US";

    public static class Entry {
        public String code;
        public String name;

        public Entry(String str, String str2) {
            this.code = str;
            this.name = str2;
        }
    }

    public static String current() {
        return current;
    }

    public static List<Entry> languages() {
        return langs;
    }

    public static void load(Context context, String str) {
        int i;
        if (str == null || str.length() <= 0) {
            str = "en-US";
        }
        current = str;
        if (strings != null) {
            return;
        }
        try {
            InputStream open = context.getAssets().open("langs.json");
            byte[] bArr = new byte[65536];
            StringBuilder sb = new StringBuilder();
            while (true) {
                int read = open.read(bArr);
                if (read <= 0) {
                    break;
                } else {
                    sb.append(new String(bArr, 0, read, "UTF-8"));
                }
            }
            open.close();
            JSONObject jSONObject = new JSONObject(sb.toString());
            JSONArray optJSONArray = jSONObject.optJSONArray("languages");
            langs.clear();
            if (optJSONArray != null) {
                for (i = 0; i < optJSONArray.length(); i++) {
                    JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        langs.add(new Entry(optJSONObject.optString("code"), optJSONObject.optString("name")));
                    }
                }
            }
            strings = jSONObject.optJSONObject("strings");
        } catch (Exception e) {
            strings = null;
        }
    }

    public static String loadFromEnv(Context context, File file) {
        String str = "en-US";
        try {
            File file2 = new File(file, "env.txt");
            if (file2.isFile()) {
                FileInputStream fileInputStream = new FileInputStream(file2);
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
            }
        } catch (Exception e) {
        }
        load(context, str);
        return str;
    }

    public static String t(String str) {
        if (strings != null) {
            JSONObject optJSONObject = strings.optJSONObject(current);
            if (optJSONObject != null && optJSONObject.has(str)) {
                return optJSONObject.optString(str);
            }
            JSONObject optJSONObject2 = strings.optJSONObject("en-US");
            if (optJSONObject2 != null && optJSONObject2.has(str)) {
                return optJSONObject2.optString(str);
            }
        }
        return str;
    }

    public static String stage(String str) {
        if (str == null) {
            return "";
        }
        String str2 = "stage_" + str.toLowerCase();
        String t = t(str2);
        return t.equals(str2) ? str : t;
    }
}
