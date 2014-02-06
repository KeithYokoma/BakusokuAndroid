# 爆速Android第三回

Jenkins を使って自動ビルドし、DeployGate で配信するまで

## Jenkins の準備

今回は Mac のローカルに Jenkins を立てて、CI 環境を構築する。

基本的には、[この Qiita の記事](http://qiita.com/makoto_kw/items/cbe93d4ebbc35f3b43fd)にある手順を実行する。

ユーザ環境で実行する為、homebrew で Jenkins をインストールする。

`brew install jenkins`

終わったら

`cp -p /usr/local/opt/jenkins/*.plist ~/Library/LaunchAgents`

起動は

`launchctl load ~/Library/LaunchAgents/homebrew.mxcl.jenkins.plist`

プロセスを終了するには、

`launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.jenkins.plist`

これで、`http://localhost:8080` で Jenkins にアクセスができるようになる。

## Jenkins のジョブの設定

### プラグインのインストール

Android のエミュレータを Jenkins から操作すること、Gradle によるビルドと、リポジトリへのアクセスのため、以下のプラグインをインストールする。

- `Gradle Plugin`
- `Git Plugin`
- `Android Emulator Plugin`

### プラグインの設定

Android SDK のロケーションを設定する。

Android SDK Root に、SDK のルートディレクトリまでのパスを入力し、完了。

### ジョブの作成

新規ジョブ作成から、フリースタイル・プロジェクトのビルドを選択する。

プロジェクト名は任意のものを設定。

ソースコード管理から、Git を選択し、リポジトリの URL を設定する。

ビルドトリガは、強制的に定期実行する場合は定期的に実行をチェックする。もし、差分がある場合のみビルドを実行する場合は、SCM をポーリングをチェックする。
いずれにしても、定期実行する or SCM のポーリングのタイミングを設定するための、クーロン書式の設定をする必要がある。

例えば、一時間に一回実行する場合は以下のようにする。

`@hourly`

あるいは、15 分ごとに実行する場合は

`H/15 * * * *`

次に、ビルド環境から、Run an Android emulator during build をチェックし、エミュレータの設定をする。

Jenkins 上でエミュレータの設定をする場合、Run emulator with properties を選択する。  
Android OS version は、`2.3.3` や `4.0.3` のようなバージョン名を指定。  
Screen density には、dpi の数字を設定。  
Screen resolution は、WXGA や WVGA など解像度を示すものを設定。  
Device Locale は、日本語であれば ja_JP を指定。  
SD Card Size は 32MB や 64MB などの容量を入力。
Target ABI はアーキテクチャを指定する。x86 があれば x86 を指定すると高速に動作する。

Common emulator options では、自動ビルド環境のため、Show emulator window のチェックをはずす。

次に、ビルドの項目に、ビルド手順を追加する。
手順一覧から、Invoke Gradle script を選択し、必要な項目を入力する。

Tasks に、ワークスペースのクリーンアップと一連のビルドをすべて実行するためのタスクを記述する。

`clean build`

最後に、ビルド後の処理として、成果物を保存するよう設定する。

以下の成果物を保存しておくこと。

- `**/*.apk`
  アプリ本体
- `**/mapping.txt`
  ProGuard のマッピングファイル。難読化されたスタックトレースから逆引きする際に必要
- `**/seeds.txt`

ファイル指紋の記録もしておくとよい。

以上で、最低限の Jenkins でのビルド設定が完了したので、ビルドを実行してみる。

## DeployGate で配信する


