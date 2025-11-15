package com.ast.ouyalaunch;

import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    public static final List<String> GENRES = Arrays.asList("Favorites", "Casual", "Action", "Racing", "Simulation", "RPG", "Apps");

    private RecyclerView recycler;
    private View overlayLoading;
    private LinearLayout tabContainer;

    private AppAdapter adapter;
    private int currentTab = 0; // default "Favorites"
    private DataStore dataStore;
    private List<AppEntry> allApps = new ArrayList<>();
    private List<List<AppEntry>> tabLists;
    private boolean[] tabDirty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Eindeutige App-UUID basierend auf Paketnamen ---
        String packageName = getPackageName();
        String appUUID = UUID.nameUUIDFromBytes(packageName.getBytes()).toString();
        Log.d("OUYAlaunch", "App UUID = " + appUUID);


        recycler = findViewById(R.id.recycler);
        overlayLoading = findViewById(R.id.overlay_loading);
        tabContainer = findViewById(R.id.tab_container);

        dataStore = new DataStore(this);

        GridLayoutManager glm = new GridLayoutManager(this, 3);
        recycler.setLayoutManager(glm);
        recycler.setHasFixedSize(true);
        recycler.setItemAnimator(null);
        recycler.setItemViewCacheSize(30);
        recycler.addItemDecoration(new SpacingDecoration(getResources().getDimensionPixelSize(R.dimen.grid_spacing_px)));
        adapter = new AppAdapter(this, new ArrayList<AppEntry>(), this);
        recycler.setAdapter(adapter);

        setupTabs();

        // first run?
        if (!dataStore.isCacheBuilt()) {
            new ScanTask().execute();
        } else {
            allApps = dataStore.load();
            markAllTabsDirty();
            adapter.setData(filterByCurrentTab(allApps));
            recycler.post(() -> {
                // Prüfen, ob überhaupt ein View fokussiert ist
                if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                    recycler.getChildAt(0).requestFocus();
                }
            });

        }

        // 🟠 Sanfter Fade-In-Effekt beim Appstart
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f); // zunächst unsichtbar
        root.animate()
                .alpha(1f)                     // auf volle Sichtbarkeit
                .setDuration(1500)             // Dauer 1,5 Sekunden
                .setStartDelay(100)            // kleine Verzögerung für flüssigen Start
                .start();

    }

    private void setupTabs() {
        tabContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < GENRES.size(); i++) {
            final int index = i;
            TextView tv = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, tabContainer, false);
            tv.setText(GenreNames.getDisplayName(this, GENRES.get(i)));

            // Anwenden des Styles aus styles.xml
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tv.setTextAppearance(R.style.TabText);
            } else {
                tv.setTextAppearance(this, R.style.TabText);
            }

            tv.setPadding(32, 0, 32, 0);
            tv.setSelected(i == currentTab);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectTab(index);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            tv.setLayoutParams(lp);
            tabContainer.addView(tv);
        }
        updateTabBackgrounds();
    }

    private void updateTabBackgrounds() {
        for (int i = 0; i < tabContainer.getChildCount(); i++) {
            View v = tabContainer.getChildAt(i);
            v.setBackgroundColor(i == currentTab ? getResources().getColor(R.color.gray_selected) : 0x00000000);
        }
    }

    private void selectTab(int index) {
        currentTab = index;
        updateTabBackgrounds();
        adapter.setData(filterByCurrentTab(allApps));
        recycler.post(() -> {
            // Prüfen, ob überhaupt ein View fokussiert ist
            if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                recycler.getChildAt(0).requestFocus();
            }
        });

    }

    private void initTabCacheIfNeeded() {
        if (tabLists == null || tabDirty == null || tabDirty.length != GENRES.size()) {
            tabLists = new ArrayList<>();
            tabDirty = new boolean[GENRES.size()];
            for (int i = 0; i < GENRES.size(); i++) {
                tabLists.add(new ArrayList<>());
                tabDirty[i] = true;
            }
        }
    }

    private void markAllTabsDirty() {
        initTabCacheIfNeeded();
        for (int i = 0; i < tabDirty.length; i++) {
            tabDirty[i] = true;
        }
    }

    private void rebuildTabList(int tabIndex) {
        initTabCacheIfNeeded();
        if (tabIndex < 0 || tabIndex >= GENRES.size()) return;

        String genre = GENRES.get(tabIndex);
        List<AppEntry> list = tabLists.get(tabIndex);
        list.clear();

        if (allApps == null) return;

        for (AppEntry e : allApps) {
            if ("Favorites".equals(genre)) {
                if (e.favorite) list.add(e);
            } else if (genre.equals(e.genre)) {
                list.add(e);
            }
        }

        if ("Favorites".equals(genre)) {
            java.util.Collections.sort(list, new java.util.Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    String ta = (a.title != null) ? a.title.toLowerCase() : "";
                    String tb = (b.title != null) ? b.title.toLowerCase() : "";
                    return ta.compareTo(tb);
                }
            });
        }

        tabDirty[tabIndex] = false;
    }

List<AppEntry> filterByCurrentTab(List<AppEntry> ignored) {
        initTabCacheIfNeeded();

        if (tabDirty[currentTab]) {
            rebuildTabList(currentTab);
        }

        return tabLists.get(currentTab);
    }



    // ==============================
    // NEU: Löschen-Funktion
    // ==============================
    private void removeAppFromLauncher(String packageName) {
        List<AppEntry> updated = new ArrayList<>();

        for (AppEntry app : allApps) {
            if (!app.packageName.equals(packageName)) {
                updated.add(app);
            } else {
                Log.i("RemoveApp", "App entfernt: " + app.title + " (" + app.packageName + ")");
            }
        }

        allApps = updated;
        markAllTabsDirty();
        dataStore.save(allApps, true);
        adapter.setData(filterByCurrentTab(allApps));
        recycler.post(() -> {
            // Prüfen, ob überhaupt ein View fokussiert ist
            if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                recycler.getChildAt(0).requestFocus();
            }
        });

    }

    // ==============================
    // NEU: Rescan-Funktion
    // ==============================
    private void rescanAppsPreserveGenres() {
        new AsyncTask<Void, Void, List<AppEntry>>() {
            @Override
            protected void onPreExecute() {
                overlayLoading.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<AppEntry> doInBackground(Void... voids) {
                List<AppEntry> existing = dataStore.load();
                List<AppEntry> freshScan = AppScanner.buildCache(MainActivity.this);

                // 1) Merge: nur neue Pakete hinzufügen, bestehende unverändert lassen
                List<AppEntry> merged = new ArrayList<>(existing);
                for (AppEntry n : freshScan) {
                    boolean found = false;
                    for (AppEntry o : existing) {
                        if (o != null && n != null && o.packageName != null
                                && o.packageName.equals(n.packageName)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) merged.add(n);
                }

                // 2) Pro-Genre gruppieren (ohne putIfAbsent)
                java.util.HashMap<String, List<AppEntry>> genreMap = new java.util.HashMap<>();
                for (AppEntry e : merged) {
                    if (e == null) continue;
                    String g = (e.genre != null && e.genre.length() > 0) ? e.genre : "Casual";
                    List<AppEntry> bucket = genreMap.get(g);
                    if (bucket == null) {
                        bucket = new ArrayList<>();
                        genreMap.put(g, bucket);
                    }
                    bucket.add(e);
                }

                // 3) Null-sicherer Comparator (A–Z)
                java.util.Comparator<AppEntry> byTitleAZ = new java.util.Comparator<AppEntry>() {
                    @Override public int compare(AppEntry a, AppEntry b) {
                        String ta = (a != null && a.title != null) ? a.title : "";
                        String tb = (b != null && b.title != null) ? b.title : "";
                        return ta.toLowerCase().compareTo(tb.toLowerCase());
                    }
                };

                // 4) Innerhalb jedes Genres sortieren
                for (List<AppEntry> list : genreMap.values()) {
                    java.util.Collections.sort(list, byTitleAZ);
                }

                // 5) In feste Genre-Reihenfolge zurückführen
                List<AppEntry> sortedMerged = new ArrayList<>();
                for (String g : GENRES) {
                    List<AppEntry> bucket = genreMap.get(g);
                    if (bucket != null) sortedMerged.addAll(bucket);
                }
                // Apps mit unbekanntem Genre (falls vorhanden) ans Ende
                for (java.util.Map.Entry<String, List<AppEntry>> e : genreMap.entrySet()) {
                    if (!GENRES.contains(e.getKey())) {
                        sortedMerged.addAll(e.getValue());
                    }
                }

                dataStore.save(sortedMerged, true);
                return sortedMerged;
            }

            @Override
            protected void onPostExecute(List<AppEntry> result) {
                overlayLoading.setVisibility(View.GONE);
                if (result == null) {
                    result = new ArrayList<>();
                }
                allApps = result;
                markAllTabsDirty();
                adapter.setData(filterByCurrentTab(allApps));
                recycler.post(() -> {
                    // Prüfen, ob überhaupt ein View fokussiert ist
                    if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                        recycler.getChildAt(0).requestFocus();
                    }
                });

            }
        }.execute();
    }


private class ScanTask extends AsyncTask<Void, Void, List<AppEntry>> {
    @Override
    protected void onPreExecute() {
        try {
            overlayLoading.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.e("ScanTask", "onPreExecute failed", t);
        }
    }

    @Override
    protected List<AppEntry> doInBackground(Void... voids) {
        try {
            return AppScanner.buildCache(MainActivity.this);
        } catch (Throwable t) {
            Log.e("ScanTask", "Error in background scan", t);
            return new ArrayList<>();
        }
    }

    @Override
    protected void onPostExecute(List<AppEntry> appEntries) {
        try {
            if (overlayLoading != null)
                overlayLoading.setVisibility(View.GONE);

            if (appEntries == null) {
                Log.w("ScanTask", "appEntries is null, creating empty list");
                appEntries = new ArrayList<>();
            }

            dataStore.save(appEntries, true);

            allApps = appEntries;
            markAllTabsDirty();

            if (adapter != null) {
                adapter.setData(filterByCurrentTab(allApps));
                recycler.post(() -> {
                    // Prüfen, ob überhaupt ein View fokussiert ist
                    if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                        recycler.getChildAt(0).requestFocus();
                    }
                });

            } else {
                Log.w("ScanTask", "Adapter is null!");
            }

            Log.i("ScanTask", "Finished scanning, " + appEntries.size() + " apps found");

        } catch (Throwable t) {
            Log.e("ScanTask", "onPostExecute crash", t);
        }
    }
}@Override
    public void onAppLaunch(AppEntry entry) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(entry.packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onGenreChange(AppEntry entry) {
        int idx = GENRES.indexOf(entry.genre);
        if (idx < 0) idx = 1;
        idx++;
        if (idx >= GENRES.size()) idx = 1; // skip favorites
        entry.genre = GENRES.get(idx);
        dataStore.saveOne(entry);
        markAllTabsDirty();
        adapter.setData(filterByCurrentTab(allApps));
        recycler.post(() -> {
            // Prüfen, ob überhaupt ein View fokussiert ist
            if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                recycler.getChildAt(0).requestFocus();
            }
        });

    }

    @Override
    public void onToggleFavorite(AppEntry entry) {
        entry.favorite = !entry.favorite;
        dataStore.saveOne(entry);
        markAllTabsDirty();
        if (currentTab == 0) adapter.setData(filterByCurrentTab(allApps));
        recycler.post(() -> {
            // Prüfen, ob überhaupt ein View fokussiert ist
            if (recycler.getFocusedChild() == null && recycler.getChildCount() > 0) {
                recycler.getChildAt(0).requestFocus();
            }
        });

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_BUTTON_L1) {
                selectTab(Math.max(0, currentTab - 1));
                return true;
            } else if (code == KeyEvent.KEYCODE_BUTTON_R1) {
                selectTab(Math.min(GENRES.size() - 1, currentTab + 1));
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }


    // ==============================
    // NEU: L2 + R2 für Rescan
    // ==============================
    private boolean l2Pressed = false;
    private boolean r2Pressed = false;

    private long bButtonDownTime = 0;
    private static final long LONG_PRESS_THRESHOLD = 2000; // in ms
    private boolean bPressed = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L2:
                l2Pressed = true;
                if (r2Pressed) {
                    Log.i("MainActivity", "L2 + R2 gedrückt – starte erneuten Scan");
                    rescanAppsPreserveGenres();
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                r2Pressed = true;
                if (l2Pressed) {
                    Log.i("MainActivity", "L2 + R2 gedrückt – starte erneuten Scan");
                    rescanAppsPreserveGenres();
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                if (!bPressed) { // verhindert Mehrfachauslösung
                    bPressed = true;
                    bButtonDownTime = System.currentTimeMillis();
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L2:
                l2Pressed = false;
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                r2Pressed = false;
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                long pressDuration = System.currentTimeMillis() - bButtonDownTime;
                bPressed = false;
                if (pressDuration >= LONG_PRESS_THRESHOLD) {
                    AppEntry selected = adapter.getSelectedApp();
                    if (selected != null) {
                        removeAppFromLauncher(selected.packageName);
                    }
                } else {
                    Log.i("MainActivity", "Kurzer Y-Druck – keine Aktion");
                }
                return true;

        }
        return super.onKeyUp(keyCode, event);
    }



    private void refreshTabTitles() {
        // Refresh tab labels from GenreNames (JSON may have changed)
        int n = Math.min(tabContainer.getChildCount(), GENRES.size());
        for (int i = 0; i < n; i++) {
            View v = tabContainer.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setText(GenreNames.getDisplayName(this, GENRES.get(i)));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reapply possible user changes to genre_names.json
        refreshTabTitles();
    }

}