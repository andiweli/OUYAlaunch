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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scans launchable apps and builds a cache of OUYA-compatible entries, identified by the presence
 * of an "ouya_icon.png" inside the APK. Soft rule: if both a package and its ".launcher" twin exist,
 * the base package (without ".launcher") is ignored for the icon scan and only the ".launcher" app
 * is considered. This avoids duplicate entries and ensures the correct OUYA icon is used.
 *
 * NOTE: Keep Java 7 compatible (no lambdas).
 */
public class AppScanner {

    private static final String TAG = "AppScanner";
    private static final String ICON_FILE_NAME = "ouya_icon.png";
    private static final String LAUNCHER_SUFFIX = ".launcher";

    /** Build the full list of OUYA-compatible apps (has ouya_icon.png). */
    public static List<AppEntry> buildCache(Context ctx) {
        List<AppEntry> out = new ArrayList<AppEntry>();
        PackageManager pm = ctx.getPackageManager();

        try {
            // 1) Get all launchable activities
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
            if (infos == null) infos = new ArrayList<ResolveInfo>();

            // 2) Collect unique package names
            Set<String> packages = new HashSet<String>();
            for (int i = 0; i < infos.size(); i++) {
                ResolveInfo ri = infos.get(i);
                if (ri == null || ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg != null && pkg.length() > 0) {
                    packages.add(pkg);
                }
            }

            // 3) Compute skip set for base packages when a ".launcher" twin exists
            Set<String> skipBase = new HashSet<String>();
            for (String pkg : packages) {
                if (pkg.endsWith(LAUNCHER_SUFFIX)) {
                    String base = pkg.substring(0, pkg.length() - LAUNCHER_SUFFIX.length());
                    if (packages.contains(base)) {
                        skipBase.add(base);
                    }
                }
            }
            if (!skipBase.isEmpty()) {
                Log.i(TAG, "Launcher-pairs detected, will ignore bases for icon scan: " + skipBase);
            }

            // 4) Prepare icon cache dir
            File iconDir = new File(ctx.getFilesDir(), "icons");
            if (!iconDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                iconDir.mkdirs();
            }

            // 5) Iterate packages, apply soft rule, look for ouya_icon.png, and cache
            for (String pkg : packages) {
                // Soft rule: if base is paired with ".launcher", skip base
                if (skipBase.contains(pkg)) {
                    Log.d(TAG, "Skip base package due to .launcher twin: " + pkg);
                    continue;
                }

                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    if (ai == null || ai.sourceDir == null) continue;

                    // Search inside APK zip for ouya_icon.png (any density path)
                    IconMatch match = findOuyaIconInApk(ai.sourceDir);
                    if (match == null) {
                        // No OUYA icon → not considered OUYA-compatible
                        continue;
                    }

                    // Decode and cache icon file
                    InputStream is = match.open();
                    if (is == null) continue;
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp == null) continue;

                    File outPng = new File(iconDir, pkg + ".png");
                    FileOutputStream fos = new FileOutputStream(outPng);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();

                    // Build entry
                    CharSequence label = pm.getApplicationLabel(ai);
                    String title = label != null ? label.toString() : pkg;

                    AppEntry e = new AppEntry();
                    e.packageName = pkg;
                    e.title = title;
                    e.genre = "Casual";     // default; user can reassign, DataStore preserves
                    e.favorite = false;
                    e.iconPath = outPng.getAbsolutePath();

                    out.add(e);

                    Log.d(TAG, "Added OUYA app: " + pkg + " (" + title + "), icon=" + match.entryName);
                } catch (Throwable perApp) {
                    Log.w(TAG, "Failed scanning " + pkg + ": " + perApp.getMessage());
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Fehler beim App-Scan: " + t.getMessage(), t);
        }

        Log.i(TAG, "Scan abgeschlossen, " + out.size() + " OUYA-kompatible Apps gefunden.");
        // Sort A–Z by title
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                String ta = (a != null && a.title != null) ? a.title : "";
                String tb = (b != null && b.title != null) ? b.title : "";
                return ta.toLowerCase(Locale.US).compareTo(tb.toLowerCase(Locale.US));
            }
        });
        return out;
    }

    /** Represents a lazy-open match inside the APK. */
    private static class IconMatch {
        final String apkPath;
        final String entryName;
        IconMatch(String apkPath, String entryName) { this.apkPath = apkPath; this.entryName = entryName; }
        InputStream open() {
            try {
                ZipFile zf = new ZipFile(apkPath);
                ZipEntry ze = zf.getEntry(entryName);
                if (ze == null) return null;
                // NOTE: The caller decodes & closes the stream; we keep ZipFile referenced
                // until stream is closed by framework.
                return zf.getInputStream(ze);
            } catch (Throwable t) {
                Log.w(TAG, "open() failed for " + entryName + " in " + apkPath + ": " + t.getMessage());
                return null;
            }
        }
    }

    /**
     * Find the best-matching "ouya_icon.png" entry within the given APK zip path.
     * Preference order: xxxhdpi > xxhdpi > xhdpi > hdpi > mdpi > any.
     */
    private static IconMatch findOuyaIconInApk(String apkPath) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(apkPath);
            // Collect candidates
            List<String> candidates = new ArrayList<String>();
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                if (ze == null) continue;
                String name = ze.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(Locale.US);
                if (lower.endsWith("/" + ICON_FILE_NAME) || lower.equals(ICON_FILE_NAME)) {
                    candidates.add(name);
                }
            }
            if (candidates.isEmpty()) return null;

            // Rank by density hints if available
            int bestScore = -1;
            String best = null;
            for (int i = 0; i < candidates.size(); i++) {
                String n = candidates.get(i);
                String ln = n.toLowerCase(Locale.US);
                int score = densityScore(ln);
                if (score > bestScore) {
                    bestScore = score;
                    best = n;
                }
            }
            if (best == null) best = candidates.get(0);
            // Don't keep the ZipFile open; IconMatch#open will reopen on demand
            zf.close();
            return new IconMatch(apkPath, best);
        } catch (Throwable t) {
            try { if (zf != null) zf.close(); } catch (Throwable ignore) {}
            return null;
        }
    }

    private static int densityScore(String pathLower) {
        // Higher score = higher preference
        if (pathLower.contains("xxxhdpi")) return 5;
        if (pathLower.contains("xxhdpi"))  return 4;
        if (pathLower.contains("xhdpi"))   return 3;
        if (pathLower.contains("hdpi"))    return 2;
        if (pathLower.contains("mdpi"))    return 1;
        return 0;
    }
}
