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

## 写真追加時のコールバックの実装

幾つか方法がある。

### ContentObserver を使う場合

カメラで撮影された写真が、MediaStore へ保存・登録されたことを監視するための仕組みを用いる。
アプリケーションのプロセスが終了すると正しく動作しないことに注意する。

```Java
public class MyApplication extends Application {
    private ContentObserver mObserver;
    @Override
    public void onCreate() {
        super.onCreate();

        mObserver = new ContentObserver(new Handler(), this) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                // MediaStore.Images.Media.EXTERNAL_CONTENT_URI のデータベースに変更があった時のコールバック
            }
        };
        // あまりよい実装ではない…
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserver);
    }

    @Override
    public void onTerminate() {
        getContentResolver().unregisterContentObserver(mObserver);
        super.onTerminate();
    }
}
```

### BroadcastReceiver を使う場合

OS の標準カメラアプリが送出する Broadcast Intent を拾う。
カメラアプリの実装に依存することに注意する。

```xml
<receiver
    android:name="jp.yokomark.example.MyBroadcastReceiver"
    android:exported="true"
    android:enabled="true">
    <intent-filter>
        <action android:name="com.android.camera.NEW_PICTURE"/>
        <action android:name="android.hardware.action.NEW_PICTURE"/>
        <data android:mimeType="image/*"/>
    </intent-filter>
</receiver>
```

## Push 通知(GCM)

- 参考リンク
  - https://support.google.com/googleplay/android-developer/answer/2663268?hl=ja  
  - http://developer.android.com/google/gcm/client.html

### build.gradle の設定

以下の依存を追加する。

```groovy
dependencies {
    compile "com.google.android.gms:play-services:3.1.+"
}
```

### AndroidManifest の設定

以下の宣言をする。

```xml
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="jp.yokomark.example.push"
    android:versionCode="1"
    android:versionName="1.0.0">

    <!-- パーミッションの定義 -->
    <permission
        android:name="jp.yokomark.example.push.permission.C2D_MESSAGE"
        android:protectionLevel="signature"/>

    <!-- パーミッションの使用を宣言 -->
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
    <uses-permission android:name="jp.yokomark.example.push.permission.C2D_MESSAGE"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <application ...>
        <!-- Push を受け取った時のコールバックとなる BroadcastReceiver の宣言 -->
        <!-- 後述のように、Push 受信時に WakeLock を取得する場合、BroadcastReceiver は WakefulBroadcastReceiver である必要がある -->
        <!-- WakeLock の取得をしない場合は通常の BroadcastReceiver でよい -->
        <receiver
            android:name=".GcmBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVER"/>
                <category android:name="jp.yokomark.example.push"/>
            </intent-filter>
        </receiver>
        <!-- Push を受けた時に WakeLock を取得する場合、Push を受けた BroadcastReceiver が発信する Intent を受ける IntentService を登録する -->
        <service android:name=".GcmIntentService"/>
    </application>
</manifest>
```

### GCM の登録の実装

実装のサンプルは Android Developers を参照。
手順は以下のとおり。

1. Google Play Services API に依存するので、まずはその依存先の API のチェックを行う。API がなく、専用 apk をダウンロードすることで API が使用できる場合は、apk のダウンロードを促す。
2. 確認できたら、Sender Id を元に Registration Id を発行してもらい（非同期）、GCM にアプリケーションを登録する。
3. 得られた Registration Id と、登録時のアプリのバージョンをストレージに保存しておく。バージョンアップ時には再度 Registration Id を発行するようにしておく。
4. 得られた Registration Id をバックエンドに登録する。

### 受信側の実装

1. WakefulBroadcastReceiver を継承し、コールバックで WakefulService を起動する。
2. GCMBaseIntentService を継承し、GCM の登録やエラー、メッセージ受信などを受け付けるコールバックを持った IntentService を書く。
