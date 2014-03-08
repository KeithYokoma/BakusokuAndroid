# 爆速Android第四回

Robolectric によるテスト高速化と、設計、セキュリティについて

## Robolectric

Android のテストは、Android のフレームワークで提供されている API を利用する関係で、エミュレータや端末上で実行する必要があり、動作速度が通常の Java のテストよりも遅い。
Robolectric は、Android フレームワークの API をエミュレートすることで、JavaVM 上でテストを実行可能にし、テストのスピードを上げている。

基本的には、[このページ](http://www.peterfriese.de/android-testing-with-robolectric/)の手順にしたがってプロジェクトを準備する。

Robolectric は現状 KitKat に対応していないため、Target Api ならびに Compile With の API Level を 18 に設定しておく。

### ビルドスクリプトの設定

デフォルトでは、テストコードの配置は`instrumentTest`ディレクトリであるが、Robolectric では`test`ディレクトリを使用する為、依存関係と合わせて設定する。

```Groovy
buildscript {
    // ビルドスクリプトの依存
    repositories {
        mavenCentral()
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:0.8.+'
        classpath 'com.squareup.gradle:gradle-android-test-plugin:0.9.1-SNAPSHOT'
    }
}

repositories {
    mavenCentral()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

apply plugin: 'android'
apply plugin: 'android-test'

android {
    compileSdkVersion 18
    buildToolsVersion "19.0.1"

    defaultConfig {
        minSdkVersion 7
        targetSdkVersion 18
        versionCode 1
        versionName "1.0"
    }

    sourceSets {
        instrumentTest.setRoot('src/test')
    }
}

dependencies {
    compile 'com.android.support:gridlayout-v7:19.0.1'
    compile 'com.android.support:support-v4:19.0.1'
    compile 'com.android.support:appcompat-v7:19.0.1'
    testCompile 'junit:junit:4.11' // JUnit 4
    testCompile 'org.robolectric:robolectric:2.3-SNAPSHOT'
    testCompile 'com.squareup:fest-android:1.0.+' // FEST(Fixtures for Easy Software Testing) を Android で利用するためのライブラリ
    // testCompile では AndroidStudio で入力補完がされないため、依存解決のために provided を指定
    provided 'junit:junit:4.11'
    provided 'org.robolectric:robolectric:2.3-SNAPSHOT'
    provided 'com.squareup:fest-android:1.0.+'
}
```

### TestRunner の作成

次に、Robolectric 用に TestRunner を作成する。
この TestRunner は、Robolectric が test 用の`AndroidManifest.xml`を見つけられない為に、それを TestRunner が肩代わりする為に作成する。

```Java
import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;

public class RobolectricGradleTestRunner extends RobolectricTestRunner {
    public RobolectricGradleTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        String manifestProperty = System.getProperty("android.manifest");
        if (config.manifest().equals(Config.DEFAULT) && manifestProperty != null) {
            String resProperty = System.getProperty("android.resources");
            String assetsProperty = System.getProperty("android.assets");
            return new AndroidManifest(Fs.fileFromPath(manifestProperty), Fs.fileFromPath(resProperty),
                    Fs.fileFromPath(assetsProperty));
        }
        AndroidManifest appManifest = super.getAppManifest(config);
        return appManifest;
    }

}
```

以上で Robolectric の設定は終了。

### テストケースの作成

`instrumentTest` と異なり、`AndroidTestCase`を継承する必要はない。
かわりに、TestRunner の指定と、テストケースの宣言をアノテーションでしておく必要がある。

```Java
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class) // TestRunner の指定
public class MyModuleTest {
    @Test // テストケースの表明
    public void shouldPass() throws Exception {
        assertTrue(true);
    }
}

```

テストを実行するには、コマンドラインで以下を実行する。

`$ ./gradlew test`

エミュレータや端末なしでテストが実行できれば成功。

## 設計

参考：http://qiita.com/KeithYokoma/items/f19a732aad3beff9387c

### MVC

![MVC Component](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/4/architecture.001.png "MVC Component")

Android では、Controller のための基本フレームワークとして `Activity` や `Fragment` を提供している。また、View は `TextView`、`ImageView` などに代表されるような `View` のコンポーネントである。
Model については、特にデータソースに近いレイヤのフレームワークとして、`ContentProvider` や `SharedPreferences` などが提供されている。この他、I/O を行うスレッドと UI Thread とのブリッジの役目を果たす Loader という仕組みも用意されている。
ただし、この他のビジネスロジックについては自分たちで実装する範疇となる。

### 設計の戦略

MVC では、Controller や Model が太りやすく、責務が集中しがちになる。
リファクタリングや設計の戦略として、基本的には、Controller の処理は他のクラスへ委譲することが一般的で、委譲の戦略としては、View のイベントハンドリングについては、id とイベントハンドラを対応付けたマップを保持しておき、適宜 View のイベントに応じて適切なハンドラを呼び出すような実装が考えられる。
一方、Model は、その責務に含まれる範囲が幅広いため、Model の中でもレイヤわけを細かくしておく必要がある。

以下、Model のレイヤ分離の参考例を示す。

![Model Layer](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/4/architecture.002.png "Model Layer")

Loader によって非同期処理インタフェースを作る。Loader は仕組み上キャッシュのような仕組みを持つことができるが、メモリにデータを載せておくだけの役割しか持たないようにする。
Model は UI Thread で行われるビジネスロジックを記述し、Loader よりレイヤの低い Client や DataSource は、別スレッドで非同期に実行されるビジネスロジックを記述する。
Client はアクセサではあるが、データソースが複数に渡る場合もある。
DataSource は、単なるネットワーク I/O のみならず、データベースへのアクセスも含まれる。また、キャッシュ（特にディスクキャッシュ）もデータソースとして捉え、このレイヤにアクセスする Client で適宜ハンドリングを行う。

![Model Layer 2](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/4/architecture.003.png "Model Layer 2")

### 各レイヤ間のコミュニケーション

方法論としては 2 つのやり方があり、1 つには、インタフェースを宣言して、結果を受け取る側がそのインタフェースを実装、また、依頼を受ける側に適宜そのインタフェースを実装したクラスのインスタンスのライフサイクル管理を委譲する。
もう 1 つには、EventBus を用いて間接的に各種のオペレーションフローを作る方法もある。

もっとも原始的なやり方は 1 で、以下のような図に示される関係をインタフェースに落としこんで実装する。

![MVC Communication Interface](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/4/architecture.004.png "MVC Communication Interface")

この方法の場合、インタフェースがはっきりと宣言され、実装されるので、手間はかかるが堅牢な設計と実装になる。
一方、インタフェースを変更するコストは高い。また、インタフェースは主にコールバックとして用いられる為、コールバックのライフサイクルを考慮する必要性と、コールバックインタフェースの乱立によるコールバック地獄や、誤った継承をしたときのインタフェース汚染などの危険性がある。

インタフェースを宣言した場合の欠点を解消する方法が、もう一つの EventBus を用いた設計である。

![EventBus](https://raw.github.com/KeithYokoma/BakusokuAndroid/master/docs/4/architecture.005.png "EventBus")

コールバックインタフェースの呼び出しと受け取りの部分を、EventBus を介して行うことにより、各レイヤ間の強参照がなくなる。このため、EventBus を介してやりとりされる Event オブジェクトを変更するだけでインタフェースの交換が可能となる。
ただし、この場合、インタフェースによる実装の強制がないため、抜け漏れに注意しなければならない。

- 参考リンク
  - http://d.hatena.ne.jp/pokarim/20101226
  - http://www.infoq.com/jp/news/2013/09/reactive-programming-emerging

### Activity と Fragment

おおまかに、タブレットとハンドセット端末で、機能的にはほぼ同じ機能を提供することを前提としている場合、Fragment を用いた実装をすることが望ましい。

例えば、何かしらのコンテンツの一覧画面から、その一覧のなかの詳細を見る画面へ遷移するような場面の場合、ハンドセット端末では、一覧画面と詳細画面を別々の画面として遷移させたいが、タブレットでは、左端に一覧を表示しておき、適宜その選択によって詳細画面を右側に表示し、切り替えるような場合に、Fragment が活躍する。

この場合、一覧画面も詳細画面も、Activity はほとんど責務を持たず、Fragment がモデルとのやりとりをして表示までを行うことになる。

## セキュリティ

ここでは、アプリの実装をする際に考慮すべきセキュリティ事項について解説する。

- 参考リンク
  - http://www.jssec.org/dl/android_securecoding.pdf

### IntentFilter

IntentFilter とは、Intent を受け取ることの出来るコンポーネントが、具体的にどの種類の Intent なら受け付けられるかを表すもの。
Android では、他のアプリへ Intent を飛ばすこともでき、この時 IntentFilter で宣言した種類にマッチすると、Intent が外部から受け取ることができるようになる。

基本的に、IntentFilter を宣言すると、そのコンポーネントはデフォルトで外部公開状態となり、IntentFilter の条件にマッチする限りすべての Intent を受け取ることになる。
実装にもよるが、外部からの Intent を受け取って、何らかのネットワークアクセスを行ったりするような場合、そのコンポーネントに何千回と Intent を送りつけられてしまうと、その分だけネットワークアクセスが発生し、アプリを踏み台にした攻撃が可能になってしまう。

よって、IntentFilter を宣言したコンポーネントの公開状態には気をつけなければならない。
アプリ内でしかやりとりしない Intent であれば、外部公開をしないよう設定（`android:exported="false"`にする）か、パーミッションを定義して、許可のあるアプリからの Intent のみ受け取るようにする。

### Broadcast Intent

また、自分のアプリが送信する Intent にも注意する。
Intent の中身、特に Extra にセンシティブな情報を含む場合で、かつその Intent を broadcast する場合、特になにもしないと、すべてのアプリでその Intent を受け取れるようになる。
Broadcast された Intent を解析され、IntentFilter にマッチする条件が判明すれば、Extra が引きぬくことができるようになる。

基本的には、特に事情がない限り、Broadcast は `LocalBroadcastManager` を介して行うべき。それが不可能な場合は、パーミッションを定義すること。

### ストレージ

SharedPreferences を始め、ディスクにファイルを書き込む API には、ファイルの権限設定を引数に渡すものがある。
公式にも、そのようなすべての API では、自分のアプリ以外からの書き込み・読み込みを禁止する権限にしておくことが推奨されているが、一部権限設定ができない API がある。

SD カードなどの External Storage は、権限設定のできないストレージであり、すべてのアプリが読み込み・書き込みの権限を持つことになる。
このため、機密性の高いデータは External Storage に保存してはならない。
