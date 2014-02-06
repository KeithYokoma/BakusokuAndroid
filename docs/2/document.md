# 爆速Android第二回

署名して、テーマをつくって、apk を作れるようになるまで。

## 署名する

Android の apk は、電子署名で署名されたものでなければインストール出来ない仕様になっている。
通常、開発時には、開発用の電子署名を使って自動で署名されている。

### 署名を作る

以下のコマンドで作成する。

```shell
$ keytool -genkey -v -keystore hoge.keystore -alias hoge -validity 10000
キーストアのパスワードを入力してください:  
新規パスワードを再入力してください: 
姓名を入力してください。
[Unknown]:  Hoge Fuga
組織単位名を入力してください。
[Unknown]:  Company
組織名を入力してください。
[Unknown]:  mixi
都市名または地域名を入力してください。
[Unknown]:  Tokyo
州名または地方名を入力してください。
[Unknown]:  Tokyo
この単位に該当する 2 文字の国番号を入力してください。
[Unknown]:  jp
CN=Hoge Fuga, OU=Company, O=mixi, L=Tokyo, ST=Tokyo, C=jp でよろしいですか?
[no]:  yes

1,003 日間有効な 1,024 ビットの DSA の鍵ペアと自己署名型証明書 (SHA1withDSA) を生成しています
ディレクトリ名: CN=Hoge Fuga, OU=Company, O=mixi, L=Tokyo, ST=Tokyo, C=jp
<hoge> の鍵パスワードを入力してください。
(キーストアのパスワードと同じ場合は RETURN を押してください):  
    新規パスワードを再入力してください: 
    [hoge.keystore を格納中]
```

### 署名した apk の作成

AndroidStudio の Build > Generate Signed APK... から行う。

対象のモジュールを選択し、次へ。
作成した署名ファイルのパスを入力し、パスワード、エイリアス、エイリアスのパスワードを入力する。
apk の保存先と、ProGuard(難読化と最適化)の実行有無を選択して Finish すれば、署名した apk ファイルが作成される。

これらは、毎回手動で実行するには面倒な作業なので、build.gradle にスクリプトを書いて自動化しておく。

```Groovy
android {
    // ...{snip}...

    signingConfigs {
        debug {
            // 開発時の署名設定。共通の開発用鍵を作っておくことで、ことなる開発機から同じ端末にインストールしようとしたときにも、署名によるコンフリクトが起こらずに済むので設定しておくべき
            storeFile file("conf/debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }
        release {
            // リリース用の署名設定。開発用の署名ファイルはリポジトリに入れても良いが、リリース用はリポジトリに入れないように工夫する。
            storeFile file("conf/release.keystore")
            storePassword "foobar"
            keyAlias "baz"
            keyPassword "foobar"
        }
    }

    buildTypes {
        debug {
            // 開発用ビルド時に実行する各種の処理
            debuggable true  // デバッグフラグ
            zipAlign true  // zip アライン(apk ファイルの最適化)
            runProguard false  // ProGuard の実行
            signingConfig signingConfigs.debug  // 署名の設定
        }
        release {
            // リリースビルド時の処理
            debuggable false
            zipAlign true
            runProguard true
            proguardFile getDefaultProguardFile('proguard-android-optimize.txt')
            signingConfig signingConfigs.release
        }
    }
}
```

## テーマを作る

アプリ全体のスタイルを決める。

古い OS 向けにアプリを作る場合は、互換性を保つための工夫が必要になる点に注意する。

スタイルを定義する xml
は、`{ProjectRoot}/{Module}/src/main/res/values/styles.xml`,
    `{ProjectRoot}/{Module}/src/main/res/values-v14/styles.xml`
にある。

テーマのベースは、support-v7-appcompat を使用する場合
`@style/Theme.AppCompat.Light` とする。

以下に例を示す。v-14 に切り分ける対象は、API Level が 11 ないし 14
以降のもので、古い OS 向けには、`android:` ネームスペースをつけないでおく。

`values/styles.xml`

```xml
<style name="Theme.Example" parent="@style/Theme.AppCompat.Light">
  <item name="actionBarItemBackground">@drawable/selectable_background_example</item>
  <item name="popupMenuStyle">@style/PopupMenu.Example</item>
  <item name="dropDownListViewStyle">@style/DropDownListView.Example</item>
  <item name="actionBarTabStyle">@style/ActionBarTabStyle.Example</item>
  <item name="actionDropDownStyle">@style/DropDownNav.Example</item>
  <item name="actionBarStyle">@style/ActionBar.Solid.Example</item>
  <item name="actionModeBackground">@drawable/cab_background_top_example</item>
  <item name="actionModeSplitBackground">@drawable/cab_background_bottom_example</item>
  <item name="actionModeCloseButtonStyle">@style/ActionButton.CloseMode.Example</item>
</style>
```

`values-v14/styles.xml`

```xml
<style name="Theme.Example" parent="@style/Theme.AppCompat.Light">
  <item name="android:actionBarItemBackground">@drawable/selectable_background_example</item>
  <item name="android:popupMenuStyle">@style/PopupMenu.Example</item>
  <item name="android:dropDownListViewStyle">@style/DropDownListView.Example</item>
  <item name="android:actionBarTabStyle">@style/ActionBarTabStyle.Example</item>
  <item name="android:actionDropDownStyle">@style/DropDownNav.Example</item>
  <item name="android:actionBarStyle">@style/ActionBar.Solid.Example</item>
  <item name="android:actionModeBackground">@drawable/cab_background_top_example</item>
  <item name="android:actionModeSplitBackground">@drawable/cab_background_bottom_example</item>
  <item name="android:actionModeCloseButtonStyle">@style/ActionButton.CloseMode.Example</item>
</style>
```

## ステージング用と本番用とを切り替える

### ビルドスクリプト

Gradle のスクリプトで、ステージング用、本番用などの切り替えができる。
バックエンドを手軽に切り替えることができ、また、apk もそれぞれで作成できるので、DeployGate などに配信する際も便利に使える。

```Groovy
android {
    // ... {snip} ...

    // 用途の宣言
    productFlavors {
        staging {
            proguardFile 'proguard-rules.txt'
            packageName "jp.yokomark.sample.staging"
        }
        production {
            proguardFile 'proguard-rules.txt'
            packageName "jp.yokomark.sample"
        }
    }
}
```

これで、stagingDebug, stagingRelease, productionDebug, productionRelease の 4 種類のビルドが可能となる。
それぞれ、ステージング向きデバッグ用、ステージング向きリリース用、プロダクション向きデバッグ用、プロダクション向きリリース用の apk が作られる。

パッケージ名を変えることができるので、ステージング向きとプロダクション向きのそれぞれのアプリを同じ端末にインストールすることが出来る。

### ディレクトリ構成

ステージング向きにした時には、ステージング向き専用のコード、プロダクション向きにした時は、プロダクション向き専用のコードに切り替えることが出来る。

`{ProjectRoot}/{Module}/src`以下に、productFlavors に宣言したフレーバーと同じ名前のディレクトリを作成し、その下に、java のソースコードやリソースを配置することで、それぞれに専用のコード・リソースを配置することが出来る。

- ステージング向き
  `{ProjectRoot}/{Module}/src/staging/java`
  `{ProjectRoot}/{Module}/src/staging/res`
- プロダクション向き
  `{ProjectRoot}/{Module}/src/production/java`
  `{ProjectRoot}/{Module}/src/production/res`

これらより下のディレクトリ構成は通常のものと同じでよい。

### AndroidStudio でビルドする

左端の Build Variants から、目的のフレーバーを選択する。

![Build Variants](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/2/build_variants.png "Build Variants")

選択したら、Run で自動的にビルドが開始される。
