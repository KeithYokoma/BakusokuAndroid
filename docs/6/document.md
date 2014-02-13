# 爆速Android第六回

細かい実装ノウハウについて

## Splash 画面の実装

Splash 用テーマを設定する。

```xml
<resources xmlns:android="http://schemas.android.com/apk/res/android">
    <style name="SplashScreenTheme" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="windowBackground">@drawable/image_background</item>
        <!-- ... -->
    </style>
</resources>
```

Splash 画面の Activity の宣言で、テーマを適用する。

```xml
<activity
    android:name="jp.yokomark.sample.home.ui.SplashScreenActivity"
    android:label="@string/app_name"
    android:theme="@style/SplashScreenTheme"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 自動アップロードの実装

### ContentObserver を使う場合

### FileObserver を使う場合

### BroadcastReceiver を使う場合

## Push 通知(GCM)

https://support.google.com/googleplay/android-developer/answer/2663268?hl=ja
