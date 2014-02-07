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
    instrumentTestCompile 'junit:junit:4.11'
    instrumentTestCompile 'org.robolectric:robolectric:2.3-SNAPSHOT'
    instrumentTestCompile 'com.squareup:fest-android:1.0.+'
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



## セキュリティ
