# 爆速Android第四回

Robolectric によるテスト高速化と、設計、セキュリティについて

## Robolectric

Android のテストは、Android のフレームワークで提供されている API を利用する関係で、エミュレータや端末上で実行する必要があり、動作速度が通常の Java のテストよりも遅い。
Robolectric は、Android フレームワークの API をエミュレートすることで、JavaVM 上でテストを実行可能にし、テストのスピードを上げている。

基本的には、[このページ](http://www.peterfriese.de/android-testing-with-robolectric/)の手順にしたがってプロジェクトを準備する。

## 設計

## セキュリティ
