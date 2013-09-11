package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.apache.coyote.AbstractProtocol;

/**
 * HTTPはローカルマシンからの接続のみ(Loopback)とし、 
 * HTTPSは他マシンからの接続も可とする、1つのウェブアプリケーションの構成例.<br>
 */
public class SimpleServerConfigurator2 extends AbstractServerConfigurator {

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(SimpleServerConfigurator2.class.getName());

    /**
     * リスンしているポート.<br>
     * まだバインドしていなければ0.<br>
     */
    private int listenPort = 0;
    
    /**
     * ローカルマシンでのバインドに限定するようにコネクタを構成したTomcatを構成する.<br>
     */
    @Override
    public void init() throws IOException, ServletException {
        if (tomcat != null) {
            throw new IllegalStateException("already initialized");
        }

        // Tomcatのバージョンを確認する.
        logger.log(Level.INFO, "Tomcat version=" + ServerInfo.getServerInfo());

        // 基本的なTomcatの構成のヘルパクラスを構築する.
        // (このあとで必要に応じて構成をカスタマイズする.)
        tomcat = new Tomcat();

        // リスンするポートの指定
        tomcat.setPort(-1);

        // --------------------------------
        // ベースディレクトリの指定.
        // --------------------------------
        // CATALINA_HOME, CATALINA_BASEシステムプロパティの設定.
        // このディレクトリ下でJSPのコンパイルやセッションの永続化など
        // さまざまなデータの保存が行われる.
        String baseDir = new File(getAppRootDir(), "work").getCanonicalPath();
        tomcat.setBaseDir(baseDir);

        // --------------------------------
        // ローカルマシン専用とするため、
        // 独自のコネクタとスレッドプールを設定する
        // --------------------------------
        initConnectors();

        // --------------------------------
        // クラスローダを設定する.
        // JavaFXのローダと組み合わせる場合は必須である.
        // Tomcatを構築するクラス自身がJavaFXの独自のクラスローダ上にいるため、
        // クラスローダーの関係性については十分に注意が必要となる.
        // --------------------------------
        ClassLoader cl = AbstractServerConfigurator.class.getClassLoader();
        logger.log(Level.INFO, "setParentClassLoader: " + cl);
        tomcat.getEngine().setParentClassLoader(cl);
        tomcat.getHost().setParentClassLoader(cl);
        tomcat.getServer().setParentClassLoader(cl);
        tomcat.getService().setParentClassLoader(cl);

        // --------------------------------
        // ウェブアプリケーションを設定する
        // --------------------------------
        initWebApp();

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
     * ウェブアプリケーションと、そのディレクトリの設定を行う.
     *
     * @throws IOException
     * @throws ServletException
     */
    protected void initWebApp() throws IOException, ServletException {
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
    }

    /**
     * 2つ以上のコネクタと、それに関連づけるスレッドプールの設定
     *
     * @throws IOException
     * @throws SocketException
     */
    protected void initConnectors() throws IOException, SocketException {
        // ---------------------------------------------------------
        // スレッドプールの設定
        StandardService service = (StandardService) tomcat.getService();

        StandardThreadExecutor executor1 = new StandardThreadExecutor();
        executor1.setName("executor1");
        executor1.setNamePrefix("executor1-");
        executor1.setMaxThreads(20); // 最大スレッド数
        executor1.setMinSpareThreads(2); // 最低スレッド数
        executor1.setMaxIdleTime(60 * 1000); // 縮退までのアイドル時間
        service.addExecutor(executor1);

        // ---------------------------------------------------------
        // コネクタの設定
        // Tomcatクラスでは、start時にコネクタが未設定の場合はデフォルトのコネクタを必ず作成する.
        // そのため事前にダミーのコネクタ(ポート指定なし)を設定しておくことで、無効化しておく.
        tomcat.setConnector(new Connector("HTTP/1.1"));

        // リスンするポートを自動設定とする
        listenPort = 0;

        // Loopback(localhost)にのみバインドする.
        // IPアドレスを限定してソケットをバインドする場合、
        // IPv6用, IPv4用で、それぞれ異なるコネクタが必要となる.
        Collection<InetAddress> loopbackAddresses = getLoopbackAddresses();
        for (InetAddress loopbackAddress : loopbackAddresses) {
            // コネクタとして指定可能なプロトコルは以下のとおり.
            // org.apache.coyote.http11.Http11Protocol - blocking Java connector
            // org.apache.coyote.http11.Http11NioProtocol - non blocking Java
            // connector
            // org.apache.coyote.http11.Http11AprProtocol - the APR/native
            // connector
            // デフォルトは"HTTP/1.1"が設定され、blocking or Aprのいずれかが選択される.

            // その他のパラメータについては以下URLを参照
            // http://tomcat.apache.org/tomcat-7.0-doc/config/http.html

            final Connector connector = new Connector("HTTP/1.1");
            setExecutor(connector, executor1);

            enableCompression(connector);
            connector.setPort(-1); // 初期化開始時まではポートは未設定とする.

            // バインドするアドレスを指定
            String address = loopbackAddress.getHostAddress();
            connector.setAttribute("address", address);
            logger.log(Level.INFO, "bind address=" + address);

            // stopでunbindする為
            connector.setAttribute("bindOnInit", "false");
            service.addConnector(connector);

            // コネクタの開始・停止のイベントでポートの割り当てを制御するためのリスナ
            connector.addLifecycleListener(new LifecycleListener() {
                @Override
                public void lifecycleEvent(LifecycleEvent event) {
                    String state = event.getType();
                    int actualPort = connector.getLocalPort();
                    logger.info("Connector " + state + "/" +
                            connector + "/actualPort=" + actualPort);

                    if (Connector.BEFORE_START_EVENT.equals(state)) {
                        // 開始前イベント.
                        // 最初のコネクタであれば "0"(Auto)を指定する.
                        // 以降のコネクタであれば最初のコネクタの実際のポートと
                        // 同じポート番号を指定する.
                        connector.setPort(listenPort);

                    } else if (Connector.AFTER_START_EVENT.equals(state)) {
                        // 開始後イベント.
                        // 最初のコネクタであれば、自動設定された実際のポートを記録する.
                        if (actualPort > 0 && listenPort <= 0) {
                            listenPort = actualPort;
                        }

                    } else if (Connector.AFTER_STOP_EVENT.equals(state)) {
                        // 停止後したのでリスンポートは初期値(0=Auto)とする.
                        listenPort = 0;
                    }
                }
            });
        }
    }
    
    /**
     * 実際にリスンしているポートを返す.<br>
     * まだリスンしていない場合は0を返す.<br>
     */
    @Override
    public int getPort() {
        return listenPort;
    }

    /**
     * ローカルループバックのアドレス.<br
     * . 存在しない場合は空.<br>
     *
     * @return ローカルループバックのアドレス
     * @throws SocketException
     */
    protected Collection<InetAddress> getLoopbackAddresses() throws SocketException {
        ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            if (netint.isLoopback()) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    addresses.add(inetAddress);
                }
                break;
            }
        }
        return addresses;
    }

    /**
     * コネクタにスレッドプールを関連づける.<br>
     * コネクタそのものではなく、その内包するプロトコルハンドラに指定する必要がある.<br>
     * [参考]
     * http://alvinalexander.com/java/jwarehouse/apache-tomcat-6.0.16/java/org
     * /apache/catalina/startup/ConnectorCreateRule.java.shtml
     *
     * @param con
     * @param executor
     */
    protected void setExecutor(Connector con, Executor executor) {
        org.apache.coyote.AbstractProtocol protocol = (AbstractProtocol) con.getProtocolHandler();
        protocol.setExecutor(executor);
    }

    /**
     * コネクタに対してgzip圧縮転送を有効化する.
     *
     * @param connector
     */
    protected void enableCompression(Connector connector) {
        connector.setProperty("compression", "on");
        connector.setProperty("compressableMimeType",
                "text/html,text/xml,text/plain,text/javascript,application/javascript");
    }

    /**
     * エントリポイント
     *
     * @param args
     */
    public static void main(String[] args) {
        EmbeddedServerFrame.launch(new SimpleServerConfigurator2());
    }
}
