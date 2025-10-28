
package com.ast.ouyalaunch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.List;

public class PackageChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // On package add/remove, rescan and merge new apps
        List<AppEntry> scanned = AppScanner.buildCache(context);
        DataStore store = new DataStore(context);
        List<AppEntry> existing = store.load();
        for (AppEntry e : scanned) {
            boolean found = false;
            for (AppEntry old : existing) {
                if (old.packageName.equals(e.packageName)) { found = true; break; }
            }
            if (!found) existing.add(e);
        }
        store.save(existing, true);
    }
}
