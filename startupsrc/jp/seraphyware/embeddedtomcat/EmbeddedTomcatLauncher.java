package jp.seraphyware.embeddedtomcat;

/**
 * JavaFXの配備時にアプリケーションを起動するスタブとなるクラス.<br>
 * <br>
 * アプリケーション本体は、TomcatやDerbyなどの各種ライブラリのjarと同じクラスローダで
 * 読み込まれるように、あらかじめjar化し、JavaFXのリソースとしてパッケージングし、
 * JavaFXアプリとしては本クラスのみを格納する.<br>
 * <br>
 * JavaFXのローダを用いる場合、リソースとして指定されたJarは、通常とは異なり、JavaFXによる、
 * URLClassLoaderによって読み込まれ、メインjar内(AppClassLoader)のクラスローダと分離される.<br>
 * さらにサーブレットコンテナによるコンテキストごとのクラスローダによって
 * ServletやFilterなどが分離されるため、JavaFX上でのクラスローダの関係は通常より複雑となる.<br>
 * <br>
 * 問題を避けるには、DerbyやTomcatなどの各種jarと本アプリのクラスが同じクラスローダで読み込
 * まれるようにアプリ本体をjarにしてJavaFXのリソースにしてしまうのが簡単な対応方法となる.<br>
 */
public class EmbeddedTomcatLauncher {

    /**
     * JavaFXの配備時のエントリポイント
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SimpleServerConfigurator4.main(args);
    }
}
