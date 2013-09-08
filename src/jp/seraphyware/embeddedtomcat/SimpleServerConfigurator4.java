package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;

/**
 * ウェブアプリケーションの構成をweb.xmlではなく、プログラム的にカスタマイズする場合の方法例.<br>
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
    }

    /**
     * コンテキストにフィルタを追加する.<br>
     */
    protected void addFilters() {
        // フィルタ名とクラスを定義する.
        // クラスローダが分離しており、実行時点ではロードされていないため、
        // ウェブアプリ内で定義されているMyServletFilterクラスは文字列としてクラス名を指定する必要がある.
        FilterDef myServletFilterDef = new FilterDef();
        myServletFilterDef.setFilterClass("jp.seraphyware.embeddedtomcat.servlet.MyServletFilter");
        myServletFilterDef.setFilterName("MyServletFilter");

        // フィルタパラメータの設定
        myServletFilterDef.addInitParameter(
                "createTime",
                Long.toString(System.currentTimeMillis()));

        // フィルタマッピングの設定
        FilterMap myServletFilterMap = new FilterMap();
        myServletFilterMap.setFilterName("MyServletFilter");
        myServletFilterMap.addURLPattern("*");

        // コンテキストにフィルタ定義とマッピング定義を追加する.
        ctx.addFilterDef(myServletFilterDef);
        ctx.addFilterMap(myServletFilterMap);
    }

    /**
     * リクエストごとにリクエスト属性をカスタマイズするために、 リクエストイベントリスナをコンテキストに設定する.
     */
    protected void addRequestEventListeners() {

        // ランダム値を生成するためのジェネレータ
        // (現状、一応、SecureRandomのnextBytesはsynchronizedされているのでスレッドセーフといえる.)
        // http://stackoverflow.com/questions/1461568/is-securerandom-thread-safe
        final SecureRandom rng = new SecureRandom();

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
                byte[] r = new byte[20];
                rng.nextBytes(r);
                req.setAttribute("random", DatatypeConverter.printHexBinary(r));
            }
        });
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
