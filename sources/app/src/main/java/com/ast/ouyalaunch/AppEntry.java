
package com.ast.ouyalaunch;

public class AppEntry {
    public String packageName;
    public String title;
    public String genre; // one of Casual, Action & Shooter, Racing, Simulation, RPG
    public boolean favorite;
    public String iconPath; // cached icon file path

    public AppEntry() {}

    public AppEntry(String pkg, String title, String genre, boolean favorite, String iconPath) {
        this.packageName = pkg;
        this.title = title;
        this.genre = genre;
        this.favorite = favorite;
        this.iconPath = iconPath;
    }
}
