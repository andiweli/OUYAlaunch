# OUYAlaunch
Lightweight and fast launcher for the OUYA Android micro-gaming-console.

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/ffa3af32-f796-4e6e-acfc-8233935abb7f" />

## Description

**OUYAlaunch** is my attempt in developing an Android Launcher for the OUYA Console<br/>
It is focused on a clean UI, OUYA colors, fast and lightweight.
The UI was primary designed in Photoshop with some handdrawn button graphics and logos.

### First launch

At the first launch OUYAlaunch scans ALL installed apps for an ``ouya_icon.png`` file somewhere in ``res/`` and caches them for fast loading.

<img width="1280" height="720" alt="image" src="https://github.com/user-attachments/assets/c7af91ad-d01c-4ac7-b2c8-02adfbe67a5d" />

As per default ALL scanned apps are initially stored in the 2nd tab "Casual".

### Now for the available options
OUYAlaunch has no specific "Settings" or "Options" menu.<br/>
You control everything with the OUYA-controller-buttons:

O = launch a selected app<br/>
U = move the selected app to the next genre<br/>
Y = delete an app (you have to press the button 2 seconds or longer)<br/>
A = set an app as "Favorite"

L1 and R1 navigate your thru the tabs.<br/>
L2 and R2 both pressed the same time initiate a rescan of all installed apps.

DPAD or Analog sticks navigate thru the app-grid.

### Renaming "Genre-tabs" (since version 1.5)

Customize genre names in the JSON file located at ``sdcard/android/data/com.ast.ouyalaunch``

Define your own 6 genres in the above mentioned file. **The config file will be created the first time the launcher scans your apps!**  
You just need a texteditor - or edit it on the fly with Total Commander on your OUYA. Just edit the variable names for the 6 genres. Don't edit "Genre1" up to "Genre6" :)

```
{
"Favorites": "Favorites",
"Genre1": "Casual",
"Genre2": "Action",
"Genre3": "Racing",
"Genre4": "Simulation",
"Genre5": "RPG",
"Genre6": "Apps"
}
```

## Compile

The sourcecode is optimized for Android SDK 16.<br/>
I used Android Studio 4.0.1 [available here](https://developer.android.com/studio/archive) for better compatibility.
Load the code, Build sources and compile the APK.
That's all.

## Read-to-use APK

You can also just simply download the latest APK release here.

You can find the [Changelog here](https://github.com/andiweli/OUYAlaunch/blob/main/changelog.txt).
