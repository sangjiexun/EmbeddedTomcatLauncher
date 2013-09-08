package jp.seraphyware.embeddedtomcat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Tomcatの起動・停止を制御し、 JavaFXによるウェブビューで、自分自身のウェブアプリを 表示できるようにしたメインウィンドウ.<br>
 * <br>
 * [JavaFX-Swingの連携の参考元(公式ドキュメント)]<br>
 * http://docs.oracle.com/javafx/2/swing/swing-fx-interoperability.htm#CHDIEEJE
 */
public class EmbeddedServerBrowserFrame extends EmbeddedServerFrame {

    private static final long serialVersionUID = 5658012826091835239L;

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(EmbeddedServerBrowserFrame.class.getName());

    /**
     * Swingに埋め込むJavaFXのパネル
     */
    private JFXPanel jfxPanel;

    /**
     * JavaFXのウェブビュー
     */
    private WebView webView;

    /**
     * JavaFXのウェブビューの中のエンジン
     */
    private WebEngine engine;

    /**
     * コンストラクタ
     */
    public EmbeddedServerBrowserFrame(AbstractServerConfigurator configurator) {
        super(configurator);
        initBrowserComponent();
    }

    /**
     * JavaFXのWebViewを構築してSwingに組み込む
     */
    private void initBrowserComponent() {
        // JavaFXのバージョンを確認する.
        String javaFXVersion = com.sun.javafx.runtime.VersionInfo.getRuntimeVersion();
        logger.log(Level.INFO, "javaFX Version=" + javaFXVersion);

        jfxPanel = new JFXPanel();

        // JavaFXのパネルをSwingに組み込む
        Container contentPane = getContentPane();
        contentPane.add(jfxPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(1024, 600));
        pack();

        // クッキーマネージャはJavaSEの標準とする.
        // InMemoryCookieStoreが使われるのでクッキーは永続化されない.
        // 永続化する場合は、これを拡張する必要がある.
        // なお、未指定の場合は、WebView構築時にJavaFXのクッキーマネージャ
        // com.sun.webpane.webkit.network.CookieManager が使用される.
        // (こちらもクッキーは永続化されない。JavaSE標準との違いは不明)
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        // JavaFXのWebViewを設定する.
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webView = new WebView();
                engine = webView.getEngine();

                // 各種ハンドラを設定する.
                setWebEngineHandlers(engine);

                jfxPanel.setScene(new Scene(webView));
            }
        });
    }

    /**
     * フレームを破棄する.<br>
     */
    @Override
    public void dispose() {
        super.dispose();

        // 明示的に終了する.
        // JavaFXスレッド内で例外が発生した場合、暗黙的な終了が機能しなくなるため.
        // TODO: Java7時点ではJavaFXの例外ハンドラを設定する、うまい方法がない.
        // TODO: (Java8ではサポートされる予定?)
        System.exit(0);
    }

    /**
     * ウェブエンジンの各種ハンドラを設定する.
     *
     * @param engine
     *            対象のウェブエンジン
     */
    private void setWebEngineHandlers(WebEngine engine) {
        // JavaScriptのalert, confirm, prompt関数に対応するハンドラ
        engine.setPromptHandler(createPromptHandler());
        engine.setConfirmHandler(createConfirmHandler());
        engine.setOnAlert(createAlertHandler());

        // 別ブラウザウィンドウを開くハンドラ
        engine.setCreatePopupHandler(createPopupHandler(engine));
    }

    /**
     * ブラウザの別ウィンドウを開くハンドラ.<br>
     * ※ TODO: ウィンドウ名が同一であったとしても区別する方法が不明なため、 現状は、常に新しいウィンドウが開くようにしている.
     *
     * @return
     */
    private Callback<PopupFeatures, WebEngine> createPopupHandler(final WebEngine engine) {
        return new Callback<PopupFeatures, WebEngine>() {
            @Override
            public WebEngine call(PopupFeatures popupFeatures) {

                final WebView childWebView = new WebView();
                WebEngine childEngine = childWebView.getEngine();

                setWebEngineHandlers(childEngine);

                final JFXPanel center = new JFXPanel();
                center.setScene(new Scene(childWebView));

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JDialog dlg = new JDialog(EmbeddedServerBrowserFrame.this, false);
                        Container contentPane = dlg.getContentPane();
                        contentPane.setLayout(new BorderLayout());
                        contentPane.add(center, BorderLayout.CENTER);
                        dlg.pack();
                        dlg.setVisible(true);
                    }
                });

                return childEngine;
            }
        };
    }

    /**
     * JavaScriptのalert()のハンドラ.<br>
     *
     * @return
     */
    private EventHandler<WebEvent<String>> createAlertHandler() {
        return new EventHandler<WebEvent<String>>() {
            @Override
            public void handle(final WebEvent<String> stringWebEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String message = stringWebEvent.getData();
                        JOptionPane.showMessageDialog(EmbeddedServerBrowserFrame.this, message);
                    }
                });
            }
        };
    }

    /**
     * JavaScriptのconfirm()のハンドラ.<br>
     *
     * @return
     */
    private Callback<String, Boolean> createConfirmHandler() {
        return new Callback<String, Boolean>() {
            @Override
            public Boolean call(final String message) {
                final boolean[] ret = new boolean[1];
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            int res = JOptionPane.showConfirmDialog(EmbeddedServerBrowserFrame.this, message,
                                    "Confirm", JOptionPane.YES_NO_OPTION);
                            ret[0] = (res == JOptionPane.YES_OPTION);
                        }
                    });
                    return ret[0];

                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.toString(), ex);
                }
                return false;
            }
        };
    }

    /**
     * JavaScriptのprompt()のハンドラ
     *
     * @return
     */
    private Callback<PromptData, String> createPromptHandler() {
        return new Callback<PromptData, String>() {
            @Override
            public String call(PromptData promptData) {
                final String message = promptData.getMessage();
                final String[] ret = new String[1];
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ret[0] = JOptionPane.showInputDialog(EmbeddedServerBrowserFrame.this, message);
                        }
                    });
                    return ret[0];

                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.toString(), ex);
                }
                return promptData.getDefaultValue();
            }
        };
    }

    /**
     * ウェブアプリケーションのURLを開く.<br>
     * 外部のブラウザを開く代わりに、JavaFXによる自身のWebViewで開く.<br>
     */
    @Override
    protected void open() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // JavaFXのウェブビューに直接URLを指定して開く.
                String url = "http://localhost:" + port + "/";
                engine.load(url);
            }
        });
    }

    /**
     * メインウィンドウを表示する.
     *
     * @param config
     *            Tomcatの構成
     */
    public static void launch(final AbstractServerConfigurator config) {
        launch(EmbeddedServerBrowserFrame.class, config);
    }

    /**
     * エントリポイント.<br>
     * システムプロパティ"configurator"で、{@link AbstractServerConfigurator}の
     * クラス名を指定することで、サーバーの構成を切り替えることができる.<br>
     * 省略時は、{@link SimpleServerConfigurator3}が使用される.<br>
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // configuratorクラス名を取得する.(なければデフォルトを使用)
        String configClassName = System.getProperty("configurator");
        if (configClassName == null || configClassName.trim().length() == 0) {
            configClassName = SimpleServerConfigurator3.class.getCanonicalName();
        }

        // configuratorを構築する.
        @SuppressWarnings("unchecked")
        Class<? extends AbstractServerConfigurator> configClass = (Class<? extends AbstractServerConfigurator>) Class
                .forName(configClassName);
        AbstractServerConfigurator configurator = configClass.newInstance();

        // メインウィンドウを作成して開始する.
        launch(configurator);
    }
}
