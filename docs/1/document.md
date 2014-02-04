# 爆速Android第一回

AndroidStudio をインストールして、プロジェクトを作り、最初のベースを作るまで。

## AndroidStudio をインストールする

[ここ](http://developer.android.com/sdk/installing/studio.html)からダウンロードして、インストーラの指示にしたがってインストールする。

立ち上げたら、Android Studio > Check for Updates... でアップデートをインストールする。

## SDK をセットアップする

Tools > Android > SDK Manager から、SDK のセットアップを行う。

Not Installed なコンポーネントをひと通り入れておく。
開発するアプリの対象とする OS バージョンの最低ラインまでは入れておくこと。

## プロジェクトを作る

File > New Project... からプロジェクトを作るウィザードを立ち上げる。

![alt text](https://github.com/KeithYokoma/BakusokuAndroid/raw/master/src/docs/1/new_project_wizard_1.png "Wizard 1")

項目名|意味
-----|-----
Application name|アプリの名前
Module name|AndroidStudio で扱うソースコードのまとまりの名前
Package name|アプリを一意に表現するネームスペース
Minimum required SDK|サポートする最も古い OS バージョン
Target SDK|推奨動作環境を満たす最も新しい OS バージョン
Compile with|コンパイル時に使用する API
Theme|アプリのスタイル

Support Mode は、古い OS バージョン向けに使用したいバックポートコンポーネントを選択する。

![alt text](https://github.com/KeithYokoma/BakusokuAndroid/raw/master/src/docs/1/new_project_wizard_2.png "Wizard 2")

アプリを起動直後、最初の画面を設定する。

Navigation Type は、横スワイプで切り替えるか、スピナーから選択式で切り替えるか、ドロワーメニューで切り替えるかを選択する。

## 必要なライブラリを揃える

`{ProjectRoot}/{Module}/build.gradle` を編集して、ライブラリを揃える。

```Groovy
buildscript {
    repositories {
        // ここに、ライブラリが公開されているリポジトリの URL を書く。
        // デフォルトでは、maven central が登録されている。GitHub などに独自にリポジトリがある場合は、別途その URL を書く。
        mavenCentral()
        maven 'https://raw.github.com/nohana/Amalgam/master/amalgam/repository/'
    }
}

apply plugin: 'android'

// ...{snip}...

dependencies {
    // ここに、使用するライブラリの名前とバージョンを書く
    // maven で使われる表記に従い、{group-id}:{artifact-id}:{version} と記述する
    compile 'com.android.support:support-v4:19.0.0'
    compile 'com.android.support:gridlayout-v7:19.0.0'
    compile 'com.android.support:appcompat-v7:19.0.0'
    compile 'com.amalgam:Amalgam:0.2.4'
}
```

## テストの準備をする

`{ProjectRoot}/{Module}/src/instrumentTest` にテストのコードを入れるので、このディレクトリを作成する。

さらに、その下に、`{ProjectRoot}/{Module}/src/instrumentText/java` ディレクトリを作成し、この下にパッケージを作ってテストコードを書く。

```Java
package com.example.app;

import android.test.AndroidTestCase;

public class MyTest extends AndroidTestCase {
    private MyTestTarget mTarget;

    @Override
    protected void setUp() throws Exception {
        super.setUp();  // 必ず書く

        mTarget = new MyTestTarget();
        // ここで、テスト対象クラスのインスタンスを作ったり、インスタンスにモックのオブジェクトを注入したりする
    }

    @Override
    protected void tearDown() throws Exception {
        // ここで、テストが終わった後の後始末をする。I/O なら close したりなど。
        mTarget.shutDown();

        super.tearDown();  // 必ず書く
    }

    // テストケースとなるメソッドは、名前を必ず test で始める
    // テストケースとなるメソッドは、基本的に Exception をスローすることを宣言しておく
    public void testHoge() throws Exception {
        // テストする
    }
}
```
