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
            storePassword android
            keyAlias androiddebugkey
            keyPassword android
        }
        release {
            // リリース用の署名設定。開発用の署名ファイルはリポジトリに入れても良いが、リリース用はリポジトリに入れないように工夫する。
            storeFile file("conf/release.keystore")
            storePassword foobar
            keyAlias baz
            keyPassword foobar
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


