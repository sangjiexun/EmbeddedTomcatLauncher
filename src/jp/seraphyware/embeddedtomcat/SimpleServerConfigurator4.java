package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import jp.seraphyware.embeddedtomcat.data.UniqueKeyFactory;
import jp.seraphyware.embeddedtomcat.servlet.MyServletFilter;

import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.valves.AccessLogValve;

/**
 * ウェブアプリケーションの構成をweb.xmlではなく、プログラム的にカスタマイズする場合の方法例.<br>
 * また、HTTPS接続の設定と、アクセスログも追加する.<br>
 */
public class SimpleServerConfigurator4 extends SimpleServerConfigurator3 {

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(SimpleServerConfigurator3.class.getName());

    /**
     * ウェブアプリケーションと、そのディレクトリの設定を行う.
     *
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void initWebApp() throws IOException, ServletException {
        // --------------------------------
        // 自動的に構成された標準構成のHostインスタンスを取得する
        // --------------------------------
        Host host = tomcat.getHost();
        // 仮想ホストのアプリケーションディレクトリを指定する.
        // baseDirからの相対で指定するか、もしくは絶対パスを指定する.
        String appbase = new File(getAppRootDir(), "webapp2").getCanonicalPath();
        if (!new File(appbase).exists()) {
            throw new IOException("appbase is not found. " + appbase);
        }
        host.setAppBase(appbase);

        // --------------------------------
        // アプリケーションを1つ構成する.
        // --------------------------------
        // 1つのアプリだけで良いのでアプリケーションフォルダ自身とする.
        ctx = (StandardContext) tomcat.addWebapp("/", appbase);

        // --------------------------------
        // サーブレットコンテキストに値を設定する.
        // --------------------------------
        ctx.getServletContext().setAttribute("greeting", "hello, world!");

        // --------------------------------
        // フィルタを設定する. (web.xmlを使わず)
        // --------------------------------
        addFilters();

        // --------------------------------
        // リクエスト属性のイベントハンドリングする
        // --------------------------------
        addRequestEventListeners();

        // --------------------------------
        // ハイプライン処理にアクセスログを追加する.
        // --------------------------------
        initAccessLog();
    }

    /**
     * HTTP/HTTPS接続用コネクタを設定する.
     */
    @Override
    protected void initConnectors() throws IOException, SocketException {
        // HTTPアクセス用コネクタの設定
        super.initConnectors();

        // HTTPS用アクセスのためのスレッドプールの設定
        StandardService service = (StandardService) tomcat.getService();

        StandardThreadExecutor executor2 = new StandardThreadExecutor();
        executor2.setName("executor2");
        executor2.setNamePrefix("executor2-");
        service.addExecutor(executor2);

        // Java標準のKeyStoreで証明書を用いる場合は、プロトコルを明示しておく。
        // "HTTP/1.1"で指定すると、APRが有効な場合はOpenSSLによるソケットが
        // 作られるため、証明書もder, crtファイルが必要となる.
        Connector connector2 = new Connector("org.apache.coyote.http11.Http11Protocol");
        setExecutor(connector2, executor2);

        // HTTPSコネクタの設定
        connector2.setPort(443); // HTTPS
        connector2.setScheme("https");
        connector2.setSecure(true);
        connector2.setAttribute("SSLEnabled", true);
        connector2.setAttribute("sslProtocol", "TLS");
        connector2.setAttribute("bindOnInit", "false");

        // 圧縮を有効化
        enableCompression(connector2);

        // ↓クライアント証明書の必要有無
        // http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html
        connector2.setAttribute("clientAuth", "false");

        // SSL用のサーバー証明書を格納したKeyStoreを指定.
        // 以下のコマンドで自己署名の証明書を作成.
        // keytool -genkey -keyalg RSA -alias sslkey -keystore keystore.jks
        //    -storepass password -validity 3600 -keysize 2048
        String keyStoreFile = new File(getAppRootDir(), "keystore.jks").getCanonicalPath();
        connector2.setAttribute("keyAlias", "sslkey");
        connector2.setAttribute("keystorePass", "password");
        connector2.setAttribute("keystoreFile", keyStoreFile);
        service.addConnector(connector2);

        // APRを使う場合はderとcrtファイルのによる証明書の指定方法が必要.
        // SSLCertificateFile="/usr/local/ssl/server.crt"
        // SSLCertificateKeyFile="/usr/local/ssl/server.pem"
        // SSLVerifyClient="optional"
        // SSLProtocol="TLSv1"
    }

    /**
     * コンテキストにフィルタを追加する.<br>
     */
    protected void addFilters() {
        // フィルタ名とクラスを定義する.
        FilterDef myServletFilterDef = new FilterDef();
        myServletFilterDef.setFilterClass(MyServletFilter.class.getCanonicalName());
        myServletFilterDef.setFilterName(MyServletFilter.class.getSimpleName());

        // フィルタパラメータの設定
        myServletFilterDef.addInitParameter(
                "createTime",
                Long.toString(System.currentTimeMillis()));

        // フィルタマッピングの設定
        FilterMap myServletFilterMap = new FilterMap();
        myServletFilterMap.setFilterName(MyServletFilter.class.getSimpleName());
        myServletFilterMap.addURLPattern("*");

        // コンテキストにフィルタ定義とマッピング定義を追加する.
        ctx.addFilterDef(myServletFilterDef);
        ctx.addFilterMap(myServletFilterMap);
    }

    /**
     * リクエストごとにリクエスト属性をカスタマイズするために、 リクエストイベントリスナをコンテキストに設定する.
     */
    protected void addRequestEventListeners() {

        // ランダムキーを生成するファクトリ
        final UniqueKeyFactory uniquekeyFactory = new UniqueKeyFactory();

        // リクエストイベントリスナを設定する.
        // (addApplicationEventListenerメソッドは、受け取るリスナの型で、さまざまなイベントをハンドルできる.)
        ctx.addApplicationEventListener(new ServletRequestListener() {
            @Override
            public void requestDestroyed(ServletRequestEvent evt) {
                // 何もしない
            }

            @Override
            public void requestInitialized(ServletRequestEvent evt) {
                HttpServletRequest req = (HttpServletRequest) evt.getServletRequest();
                logger.info("リクエストオブジェクトを初期化します: " + req);

                // ランダム値を生成してリクエストオブジェクトにつける.
                req.setAttribute("uniqueKey", uniquekeyFactory.create());
            }
        });
    }

    /**
     * アクセスログを構成する.
     * 
     * @throws IOException
     */
    protected void initAccessLog() throws IOException {
        // --------------------------------
        // アクセスログを構成する.
        // --------------------------------
        File accessLogDir = getLogsDir();
        String logDir = accessLogDir.getAbsolutePath();
        AccessLogValve accessLogValve = new AccessLogValve();
        accessLogValve.setDirectory(logDir);
        accessLogValve.setPrefix("accesslog");
        accessLogValve.setPattern(
                org.apache.catalina.valves.Constants.AccessLog.COMBINED_ALIAS
                );
        accessLogValve.setFileDateFormat("yyyy-MM-dd");
        accessLogValve.setSuffix(".log");
        accessLogValve.setRenameOnRotate(true);
        accessLogValve.setRequestAttributesEnabled(true);
        accessLogValve.setBuffered(false);
        accessLogValve.setEnabled(true);

        ctx.addValve(accessLogValve);
        // ↓ エンジン単位に指定する場合
        // ((StandardEngine) tomcat.getEngine()).addValve(accessLogValve);
    }

    /**
     * エントリポイント
     *
     * @param args
     */
    public static void main(String[] args) {
        EmbeddedServerBrowserFrame.launch(new SimpleServerConfigurator4());
    }
}
