package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;

/**
 * 1つのポートと、1つのウェブアプリを起動する最小構成例.<br>
 */
public final class SimpleServerConfigurator1 extends AbstractServerConfigurator {

    /**
     * デフォルトのポート
     */
    private int port = 8081;

    /**
     * コンストラクタ
     */
    public SimpleServerConfigurator1() {
        // リスンするポートをシステムプロパティより取得する.
        String strPort = System.getProperty("port");
        if (strPort != null && strPort.trim().length() > 0) {
            port = Integer.parseInt(strPort);
        }
    }

    /**
     * リスンポートを返す.
     */
    @Override
    public int getPort() {
        return port;
    }
    
    /**
     * もっとも単純なTomcatの独自のスタートアップ方法による初期化.<br>
     */
    @Override
    public void init() throws IOException, ServletException {
        if (tomcat != null) {
            throw new IllegalStateException("already initialized");
        }

        // Tomcatの組み込み or テストの構成用のヘルパクラスを構築.
        tomcat = new Tomcat();

        // リスンするポートの指定
        tomcat.setPort(port);

        // --------------------------------
        // ベースディレクトリの指定.
        // --------------------------------
        // CATALINA_HOME, CATALINA_BASEシステムプロパティの設定.
        // このディレクトリ下でJSPのコンパイルやセッションの永続化など
        // さまざまなデータの保存が行われる.
        String baseDir = new File(getAppRootDir(), "work").getCanonicalPath();
        tomcat.setBaseDir(baseDir);

        // --------------------------------
        // 自動的に構成された標準構成のHostインスタンスを取得する
        // --------------------------------
        Host host = tomcat.getHost();
        // 仮想ホストのアプリケーションディレクトリを指定する.
        // baseDirからの相対で指定するか、もしくは絶対パスを指定する.
        String appbase = new File(getAppRootDir(), "webapp1").getCanonicalPath();
        host.setAppBase(appbase);

        // --------------------------------
        // アプリケーションを1つ構成する.
        // --------------------------------
        // 1つのアプリだけで良いのでアプリケーションフォルダ自身とする.
        ctx = (StandardContext) tomcat.addWebapp("/", appbase);

        // --------------------------------
        // コンテキストマネージャを設定する
        // --------------------------------
        StandardManager manager = new StandardManager();
        // セッションをファイルにシリアライズさせないためファイル名をnullにする.
        // (デフォルトではワークディレクトリ上にSESSIONS.SERファイルが生成される)
        manager.setPathname(null);
        ctx.setManager(manager);
    }

    /**
     * エントリポイント
     *
     * @param args
     */
    public static void main(String[] args) {
        EmbeddedServerFrame.launch(new SimpleServerConfigurator1());
    }
}
