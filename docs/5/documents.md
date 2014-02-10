# 爆速Android第五回

Java の各種のパラダイムやライブラリについて、WebView、UI、Dalvik

- 参考
  - http://amzn.to/1biskqi

## ThreadPoolExecutor

Android を始めとして、イベント駆動型の UI プログラミングでは、メインスレッドで UI のイベントハンドリングを行うループが走り、ネットワークやディスクへの I/O などのスレッドをブロックする処理は、ワーカスレッドへ依頼するモデルが一般的。
Android においては、AsyncTask や AsyncTaskLoader、IntentService などのコンポーネントがそれを抽象化している。

AsyncTask や AsyncTaskLoader では、内部で Thread を立ち上げ、その Thread での処理が終わり次第 MainThread に対してコールバックを返すような設計になっている。
一方で、Thread は、Thread#start() に関してパフォーマンスの問題があり、Thread のインスタンスを都度新規に作っていると、その分だけパフォーマンスが劣化する。
よって、AsyncTask や AsyncTaskLoader では、ThreadPoolExecutor という仕組みを用いて、Thread インスタンスを予めプールしておき、要求に応じてインスタンスを使いまわす実装がされている。

## 防御的コピー

ミュータブルなオブジェクト（オブジェクト生成後にその状態を変更可能なオブジェクト, 例えば Date や ArrayList など）をフィールドに持つクラスの初期化時、参照渡しで渡された引数をフィールドにセットすると、初期化処理を呼び出した側での参照先のオブジェクトの変更が、参照渡しで渡したフィールドにも影響を与えてしまう。

```Java
// Entity class
public class SomeEntry {
    private List<String> mList;

    public SomeEntry(List<String> list) {
        mList = list;
    }

    public void dump() {
        System.out.println(mList);
    }
}

// Caller
public class Main {
    public static final void main(String[] args) {
        List<String> list = new ArrayList<String>();
        list.add("hoge");
        list.add("fuga");

        SomeEntry entry = new SomeEntry(list); // SomeEntry の初期化
        entry.dump(); // [hoge, fuga]

        list.add("foo");
        list.add("bar);
        
        entry.dump(); // [hoge, fuga, foo, bar] - このメソッド呼び出しで実行した処理が、SomeEntry にも影響を与えている
    }
}
```

特に、イミュータブルな Entity を宣言する場合、参照の管理は非常に重要な設計上のポイントになる。

上記の場合、初期化時に新しくオブジェクトを作り直す（防御的コピー）ことによって、参照先のオブジェクトそのものが別モノとなるため、安全にデータを管理することができるようになる。

```Java
public class SomeEntry {
    private List<String> mList;

    public SomeEntry(List<String> list) {
        mList = new ArrayList<String>(list); // defensive copying
    }

    public void dump() {
        System.out.println(mList);
    }
}
```

もちろん、getter メソッドを宣言する場合も、防御的コピーによってデータを保護する必要がある。

```Java
public class SomeEntry {
    private List<String> mList;

    public SomeEntry(List<String> list) {
        mList = new ArrayList<String>(list); // defensive copying
    }

    public void dump() {
        System.out.println(mList);
    }

    public List<String> getList() {
        return new ArrayList<String>(mList);
    }
}
```

## 例外

- 参考リンク
  - http://d.hatena.ne.jp/daisuke-m/20081202/1228221927

Java の例外には以下の 3 種類がある。

- 検査例外(Exception)
- 非検査例外(RuntimeException)
- エラー(Error)

このうち、Error 以外の例外は、自分で設計・実装することがある（Error はシステム上の予期せぬ状態を表すので、滅多なことでは自作しない…はず…）
数々の議論があるが、おおまかに、検査例外と非検査例外の区分けは以下の様なものとなるので、これにしたがって例外の設計を行う。

- 検査例外
  try-catch ブロックによって必ず検査される例外で、設計者は、try-catch ブロックによる検査でアプリケーションは異常終了せず、縮退等をしても実行を継続可能だと考えているもの。  
  たとえば、FileNotFoundException を catch したあと、ユーザに異常を通知するものの、アプリケーション自体は終了せずそのまま実行を継続するような場面に用いられる。
- 非検査例外
  try-catch ブロックによる検査の必要のない例外で、設計者は、処理の呼び出し元に非があり、プログラミング上のミスを指摘する為に用いるもの。  
  たとえば、引数に想定外の値が渡された時に、IllegalArgumentException を投げて、プログラマにミスを知らせるような場面に用いられる。

おそらく、独自に設計する例外のほとんどは検査例外となり、プログラミング上のミスを指摘するものは既に、フレームワークで定義されているものが多い。
この設計思想では、非検査例外を catch するものは望ましくない。

また、例外は適切なレイヤにおいて適切な例外を投げることが望ましい。
たとえば、Model レイヤのクラスで、非同期にデータを読み込むメソッドの設計を考えた時、このメソッドが SocketException(プロトコルのエラーを表す例外) を投げるのはよくない実装である。
この場合、ネットワーク上でよくないことが起きたことを Model より低レイヤなクラスで例外をラップし、そのことを表現する別の例外（ex. RequestFailureException）を投げるべき。
あるいは、Model の設計として、例外を catch して適切なコールバックを呼ぶ等が考えられる。

例外をラップする際は、元の例外オブジェクトを、新しい例外オブジェクトに引き渡すことが望ましい。そうしなければ、例外の発生源の情報が失われてしまう。

```Java
try {
    // do something with network i/o
} catch (SocketException e) {
    throw new RequestFailureException(e); // 例外のラップ
}
```

## finally

リソースを取り扱うもの（I/O ストリームや Cursor など）は、たとえ処理中に異常が発生しても、確実に使用を終了し開放できるようにしておく必要がある。  
このようなときに用いられるのが、try-finally ブロック。

```Java
InputStream in = null;
try {

} finally {
    if (in != null) {
        try {
            in.close();
        } catch (IOException e) {
            // unexpected situation
        }
    }
}
```

## equals() と hashCode()

- 参考リンク
  - http://ruimo.com/publication/equalsHashCode.pdf

参照型オブジェクトの比較で用いられる。
特に、コレクションフレームワーク上で正しく動作するよう設計する場合は、equals() と hashCode() の実装には注意しなければならない。
ポイントは、equals() を継承した場合、必ず hashCode() も継承することと、equals() が `true` の場合の hashCode() の振る舞いと、equals() が `false` の場合の hashCode() の振る舞いを規約に従って実装すること。

equals() の規約は以下のとおり。

1. `null` に対する比較は常に `false`
2. 反射性を持ち、`x.equals(x)` は `true` となる
3. 対称性を持ち、`x.equals(y)` が `true` となるならば、`y.equals(x)` も `true` となる
4. 推移性を持ち、`x.equals(y)` が `true` となり、`y.equals(z)` も `true` となるならば、`x.equals(z)` も `true` となる
5. 整合性を持ち、`x.equals(y)` の返り値が、オブジェクトが同じ状態を保つ限り、何度呼び出しても同じ値を返すことを保証する

hashCode() の規約は以下のとおり。

1. equals() の判定時の状態が保たれる限り、hashCode() は同じ整数を返し続ける
2. equals() が `true` の時、比較に用いた 2 つのオブジェクトが返す hashCode() の値は同じになる
3. equals() が `false` の時、比較に用いた 2 つのオブジェクトが返す hashCode() の値は、違っていても同じであっても構わない

```Java
public class SampleEntry {
    private int mInt;
    private String mString;

    /**
     * mInt と mString が同じ値なら true
     */
    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof SampleEntry) {
            SampleEntry e = (SampleEntry) o;
            return e.getInt() == mInt &&
                    e.getString().equals(mString);
        }
        return false;
    }

    /**
     * mInt と mString.hashCode() の XOR が一番簡単
     */
    @Override
    public int hashCode() {
        return mString.hashCode() ^ mInt;
    }
}
```


