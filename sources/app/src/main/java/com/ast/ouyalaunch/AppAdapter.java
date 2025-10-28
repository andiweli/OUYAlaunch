package com.ast.ouyalaunch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {

    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnAppClickListener {
        void onAppLaunch(AppEntry entry);
        void onGenreChange(AppEntry entry);
        void onToggleFavorite(AppEntry entry);
    }

    private final Context ctx;
    private List<AppEntry> data = new ArrayList<>();
    private final OnAppClickListener listener;

    public AppAdapter(Context ctx, List<AppEntry> d, OnAppClickListener listener) {
        this.ctx = ctx;
        this.data = d;
        this.listener = listener;
    }

    public void setData(List<AppEntry> d) {
        this.data = d != null ? d : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_app, parent, false);
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setClickable(true);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AppEntry e = data.get(pos);
        h.title.setText(e.title);

        // App-Icon laden
        if (e.iconPath != null) {
            File f = new File(e.iconPath);
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bm != null) h.img.setImageBitmap(bm);
            }
        }

        // Hover/Fokus-Effekt
        h.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectedPosition = h.getAdapterPosition();
                v.setBackgroundResource(R.drawable.app_hover_bg);
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).start();
                Log.i("AppAdapter", "Fokus auf: " + e.title);
            } else {
                v.setBackgroundResource(0);
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });

        // Controller-Steuerung
        h.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    if (listener != null) listener.onAppLaunch(e);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                    if (listener != null) listener.onGenreChange(e);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    if (listener != null) listener.onToggleFavorite(e);
                    return true;
                }
            }
            return false;
        });

        // Klick mit normalem Touch
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppLaunch(e);
        });

        // Falls Adapter nach Datenänderung leer ist, Fokus auf erstes Element setzen
        if (pos == 0 && selectedPosition == RecyclerView.NO_POSITION) {
            h.itemView.requestFocus();
            selectedPosition = 0;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // 🔹 Fokus-Wiederherstellung nach Scroll
    @Override
    public void onViewAttachedToWindow(@NonNull VH holder) {
        super.onViewAttachedToWindow(holder);

        // Kein Element hat Fokus → auf sichtbares setzen
        if (selectedPosition == RecyclerView.NO_POSITION && getItemCount() > 0 && holder.itemView.getParent() != null) {
            // Nur Fokus setzen, wenn aktuell kein anderer View fokussiert ist
            ViewGroup parent = (ViewGroup) holder.itemView.getParent();
            if (parent.findFocus() == null) {
                holder.itemView.requestFocus();
                selectedPosition = holder.getAdapterPosition();
            }
        }
    // Wenn das gespeicherte Element wieder sichtbar ist → Fokus zurückgeben
        if (holder.getAdapterPosition() == selectedPosition) {
            holder.itemView.requestFocus();
        }
    }

    // 🔹 Aktuell fokussierte App abrufen
    public AppEntry getSelectedApp() {
        if (selectedPosition >= 0 && selectedPosition < data.size()) {
            return data.get(selectedPosition);
        }
        return null;
    }

    // 🔹 ViewHolder
    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title;

        public VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.title);
        }
    }
}
