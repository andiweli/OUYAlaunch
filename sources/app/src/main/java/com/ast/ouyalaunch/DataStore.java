
package com.ast.ouyalaunch;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final String FILE = "apps.json";
    private static final String FLAG = "cache.flag";

    private final Context ctx;
    private final Gson gson = new Gson();

    public DataStore(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public boolean isCacheBuilt() {
        return new File(ctx.getFilesDir(), FLAG).exists();
    }

    public void save(List<AppEntry> list, boolean setFlag) {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(gson.toJson(list));
            bw.close();
            if (setFlag) new File(ctx.getFilesDir(), FLAG).createNewFile();
        } catch (Exception ignored) {}
    }

    public void saveOne(AppEntry entry) {
        List<AppEntry> all = load();
        boolean found = false;
        for (int i=0;i<all.size();i++) {
            if (all.get(i).packageName.equals(entry.packageName)) {
                all.set(i, entry); found = true; break;
            }
        }
        if (!found) all.add(entry);
        save(all, true);
    }

    public List<AppEntry> load() {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (!f.exists()) return new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            Type listType = new TypeToken<ArrayList<AppEntry>>(){}.getType();
            List<AppEntry> list = gson.fromJson(sb.toString(), listType);
            return list != null ? list : new ArrayList<AppEntry>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
