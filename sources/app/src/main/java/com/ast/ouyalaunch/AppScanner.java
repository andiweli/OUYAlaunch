
package com.ast.ouyalaunch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AppScanner {

    public static List<AppEntry> buildCache(Context ctx) {
        List<AppEntry> out = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

            if (infos == null || infos.isEmpty()) {
                Log.w("AppScanner", "Keine Apps gefunden.");
                java.util.Collections.sort(out, new java.util.Comparator<AppEntry>() {
    @Override
    public int compare(AppEntry a, AppEntry b) {
        return a.title.toLowerCase().compareTo(b.title.toLowerCase());
    }
});

return out;
            }

            for (ResolveInfo ri : infos) {
                String pkg = ri.activityInfo.packageName;
                String label = ri.loadLabel(pm).toString();

                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    String apkPath = ai.sourceDir;

                    boolean hasOuyaIcon = false;
                    Bitmap bmp = null;

                    ZipFile zipFile = new ZipFile(apkPath);
                    for (ZipEntry entry : java.util.Collections.list(zipFile.entries())) {
                        if (entry.getName().toLowerCase().contains("drawable") &&
                            entry.getName().toLowerCase().endsWith("ouya_icon.png")) {
                            hasOuyaIcon = true;
                            InputStream is = zipFile.getInputStream(entry);
                            bmp = BitmapFactory.decodeStream(is);
                            is.close();
                            break;
                        }
                    }
                    zipFile.close();

                    if (!hasOuyaIcon || bmp == null) {
                        Log.i("AppScanner", "Überspringe " + pkg + " (kein ouya_icon.png in APK)");
                        continue;
                    }

                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, 528, 297, true);
                    File iconFile = new File(ctx.getFilesDir(), pkg + "_icon.png");
                    FileOutputStream fos = new FileOutputStream(iconFile);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();

                    AppEntry entry = new AppEntry(pkg, label, "Casual", false, iconFile.getAbsolutePath());
                    out.add(entry);

                    Log.i("AppScanner", "Gefunden: " + label + " (" + pkg + ")");

                } catch (Throwable t) {
                    Log.w("AppScanner", "Fehler bei " + pkg + ": " + t.getMessage());
                }
            }

        } catch (Throwable t) {
            Log.e("AppScanner", "Fehler beim App-Scan: " + t.getMessage(), t);
        }

        Log.i("AppScanner", "Scan abgeschlossen, " + out.size() + " OUYA-kompatible Apps gefunden.");
        java.util.Collections.sort(out, new java.util.Comparator<AppEntry>() {
    @Override
    public int compare(AppEntry a, AppEntry b) {
        return a.title.toLowerCase().compareTo(b.title.toLowerCase());
    }
});

return out;
    }
}
