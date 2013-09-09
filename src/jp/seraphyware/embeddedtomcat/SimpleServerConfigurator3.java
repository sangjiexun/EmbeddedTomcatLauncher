package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletException;

import jp.seraphyware.embeddedtomcat.db.DerbyDataSourceFactory;
import jp.seraphyware.embeddedtomcat.db.DerbyManager;

import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;

/**
 * Apache Derbyデータベースをデータソースとする、 グローバルネーミングリソース(JNDI)によるデータソースの構成例.<br>
 */
public class SimpleServerConfigurator3 extends SimpleServerConfigurator2 {

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(SimpleServerConfigurator3.class.getName());

    /**
     * Derbyデータベースの起動・終了をTomcatサーバーの起動・終了と同時に行うようにイベントを制御するように構成する.
     */
    @Override
    public void init() throws IOException, ServletException {
	// 接続等の初期化
        super.init();

        // --------------------------------
        // JNDIの有効化
        // --------------------------------
        tomcat.enableNaming();

        final StandardServer server = (StandardServer) tomcat.getServer();

        // サーバのライフサイクルリスナを設定し、
        // データベースの起動・停止を連動させる.
        server.addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                String eventType = event.getType();
                logger.log(Level.INFO, "☆" + eventType);

                if (Lifecycle.BEFORE_START_EVENT.equals(eventType)) {
                    // --------------------------------
                    // Derbyデータベースの準備
                    // --------------------------------
                    // Tomcatサーバを開始するときにDerbyも開始する.
                    initDb();

                } else if (Lifecycle.AFTER_STOP_EVENT.equals(eventType)) {
                    // --------------------------------
                    // Derbyデータベースのシャットダウン
                    // --------------------------------
                    // Tomcatサーバが停止したらDerbyも停止する.
                    closeDb();
                }
            }
        });

        // サーバのグローバルリソースにライフサイクルリスナを設定
        server.getGlobalNamingResources().addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                String eventType = event.getType();
                logger.log(Level.INFO, "★" + eventType);

                if (Lifecycle.CONFIGURE_START_EVENT.equals(eventType)) {
                    try {
                        // --------------------------------
                        // Globalネーミングの設定
                        // --------------------------------
                        // GlobalNamingResourcesはServerがスタートした後でないと設定できないため、
                        // "START_EVENT"で設定する.<br>
                        // https://github.com/jsimone/webapp-runner/blob/master/src/main/java/webapp/runner/launch/Main.java
                        applyNamingResources(server.getGlobalNamingResources());

                    } catch (NamingException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

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
    }

    /**
     * JNDIで取得する名前付きオブジェクトのグローバルリソースへの登録
     *
     * @param namingResources
     * @throws NamingException
     */
    protected void applyNamingResources(NamingResources namingResources) throws NamingException {
        // --------------------------------
        // ネーミングリソースにデータソース・環境変数を登録
        // --------------------------------
        // (factoryを指定しない場合、BasicDataSourceFactoryが用いられBasicDataSourceが生成される)
        ContextResource ctxRes = new ContextResource();
        ctxRes.setName("jdbc/ds");
        ctxRes.setAuth("Container");
        ctxRes.setType("javax.sql.DataSource");
        ctxRes.setProperty("factory", DerbyDataSourceFactory.class.getName());

        // JNDIによる環境変数
        ContextEnvironment ctxEnv = new ContextEnvironment();
        ctxEnv.setName("testvalue2");
        ctxEnv.setType("java.lang.Integer");
        ctxEnv.setValue("12345");
        ctxEnv.setOverride(false);

        namingResources.addResource(ctxRes);
        namingResources.addEnvironment(ctxEnv);

        // --------------------------------
        // リソースリンクをアプリケーションのコンテキストに設定する.
        // ここでプログラム的に構成するため、"context.xml"は不要.
        // --------------------------------
        ContextResourceLink resourceLink1 = new ContextResourceLink();
        resourceLink1.setName("jdbc/ds");
        resourceLink1.setGlobal("jdbc/ds");
        resourceLink1.setType("javax.sql.DataSource");
        ctx.getNamingResources().addResourceLink(resourceLink1);

        ContextResourceLink resourceLink2 = new ContextResourceLink();
        resourceLink2.setName("testvalue2");
        resourceLink2.setGlobal("testvalue2");
        resourceLink2.setType("java.lang.Integer");
        ctx.getNamingResources().addResourceLink(resourceLink2);
    }

    /**
     * Derbyデータベースを開始する.
     */
    protected void initDb() {
        try {
            String dbPath = new File(getAppRootDir(), "db").getCanonicalPath();
            DerbyManager.getInstance().start(dbPath);

        } catch (SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Derbyデータベースを停止する.
     */
    protected void closeDb() {
        DerbyManager.getInstance().stop();
    }

    /**
     * エントリポイント
     *
     * @param args
     */
    public static void main(String[] args) {
        EmbeddedServerFrame.launch(new SimpleServerConfigurator3());
    }
}
