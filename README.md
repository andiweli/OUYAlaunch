# OUYAlaunch
Lightweight and fast launcher for the OUYA Android micro-gaming-console.

<img width="1633" height="916" alt="image" src="https://github.com/user-attachments/assets/436c695c-9648-4ca2-bdb6-d693da24cf1f" />

## Description

**OUYAlaunch** is my first attempt in developing for Android - to be specific Android 4.1.2.<br/>
It is focused on a clean UI, OUYA colors, fast and lightweight.
The UI was primary designed in Photoshop with some handdrawn button graphics and logos.

### First launch

At the first launch OUYAlaunch scans ALL installed apps for an ``ouya_icon.png`` file somewhere in ``res/`` and caches them for fast loading.

Per default ALL scanned apps are initially stored in the tab "Casual".

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

## Compile

The sourcecode is optimized for Android SDK 16.<br/>
I used Android Studio 4.0.1 [available here](https://developer.android.com/studio/archive) for better compatibility.
Load the code, Build sources and compile the APK.
That's all.

## Read-to-use APK

You can also just simply download the latest APK release here.

You can find the [Changelog here](https://github.com/andiweli/OUYAlaunch/blob/main/changelog.txt).
