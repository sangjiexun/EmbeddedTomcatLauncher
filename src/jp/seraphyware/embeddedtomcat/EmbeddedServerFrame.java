package jp.seraphyware.embeddedtomcat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

/**
 * Tomcatの起動・停止を制御できる簡単なメインウィンドウ
 */
public class EmbeddedServerFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(EmbeddedServerFrame.class.getName());

    /**
     * Tomcatの構成オブジェクト
     */
    protected AbstractServerConfigurator configurator;

    /**
     * ステータス表示用
     */
    protected JLabel txtStatus = new JLabel();

    /**
     * 開始アクション
     */
    protected AbstractAction actStart = new AbstractAction("Start") {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            start();
        }
    };

    /**
     * 停止アクション
     */
    protected AbstractAction actStop = new AbstractAction("Stop") {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            stop();
        }
    };

    /**
     * ブラウザを開くアクション
     */
    protected AbstractAction actOpen = new AbstractAction("Open") {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            open();
        }
    };

    /**
     * コンストラクタ
     */
    public EmbeddedServerFrame(AbstractServerConfigurator configurator) {
        try {
            setTitle(getClass().getSimpleName());

            // ウィンドウを閉じるとTomcatの停止・破棄を行う.
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    close();
                }
            });

            // TOMCATの初期化
            init(configurator);

            // レイアウト
            initLayout();

        } catch (Throwable ex) {
            dispose();
            throw ex;
        }
    }

    /**
     * 画面を構成する
     */
    private void initLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(txtStatus, BorderLayout.NORTH);
        contentPane.add(createButtonPanel(), BorderLayout.SOUTH);
        updateStatus();
        pack();
    }

    /**
     * ボタンパネルを作成して返す.
     *
     * @return ボタンのパネル
     */
    protected Box createButtonPanel() {
        Box pnl = Box.createHorizontalBox();
        pnl.add(Box.createHorizontalGlue());
        pnl.add(new JButton(actOpen));
        pnl.add(new JButton(actStart));
        pnl.add(new JButton(actStop));
        pnl.add(Box.createHorizontalGlue());
        return pnl;
    }

    /**
     * 初期化
     */
    protected void init(AbstractServerConfigurator configurator) {
        if (this.configurator != null) {
            throw new IllegalStateException("already initialized");
        }
        try {
            // Tomcatを構成する.
            configurator.init();

            // 初期化されたTomcat構成、ウェブアプリケーションを取得する.
            Tomcat tomcat = configurator.getTomcat();
            StandardContext ctx = configurator.getContext();

            // --------------------------------
            // ステータスを取得するためのリスナを設定する.
            // --------------------------------
            ctx.addLifecycleListener(new LifecycleListener() {
                @Override
                public void lifecycleEvent(LifecycleEvent event) {
                    updateStatus();
                }
            });

            tomcat.getServer().addLifecycleListener(new LifecycleListener() {
                @Override
                public void lifecycleEvent(LifecycleEvent event) {
                    updateStatus();
                }
            });

            // Tomcatの構成オブジェクトを保存する.
            this.configurator = configurator;

        } catch (IOException | ServletException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * ステータスを表示しボタンの活性制御を行う.
     */
    protected void updateStatus() {
        boolean enableStart = false;
        boolean enableStop = false;
        boolean enableOpen = false;
        String statusServer = "unknown";
        String statusContext = "unknown";

        Tomcat tomcat = configurator.getTomcat();
        StandardContext ctx = configurator.getContext();
        
        if (tomcat != null) {
            LifecycleState state = tomcat.getServer().getState();
            statusServer = state.toString();

            enableStart = (state == LifecycleState.INITIALIZED || state == LifecycleState.NEW || state == LifecycleState.STOPPED);
            enableStop = (state == LifecycleState.STARTED);
        }

        if (ctx != null) {
            LifecycleState state = ctx.getState();
            statusContext = state.toString();

            enableOpen = (state == LifecycleState.STARTED);
        }

        actStart.setEnabled(enableStart);
        actStop.setEnabled(enableStop);
        actOpen.setEnabled(enableOpen);
        txtStatus.setText("srv=" + statusServer + " / ctx=" + statusContext);
    }

    /**
     * 開始ボタン
     */
    protected void start() {
        try {
            // Tomcatの開始
            Tomcat tomcat = configurator.getTomcat();
            tomcat.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 停止ボタン
     */
    protected void stop() {
        try {
            // Tomcatの停止
            Tomcat tomcat = configurator.getTomcat();
            tomcat.stop();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ウィンドウを閉じる
     */
    protected void close() {
        stop();
        try {
            // Tomcatの破棄
            // デフォルトではdestroyしないとコネクタのポートは解放されない
            Tomcat tomcat = configurator.getTomcat();
            tomcat.destroy();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
    }

    /**
     * ブラウザを開く
     */
    protected void open() {
        try {
            int port = configurator.getPort();
            String url = "http://localhost:" + port + "/";

            // システム標準のブラウザで開く.
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI(url));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(EmbeddedServerFrame.this, ex.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * メインウィンドウを表示する.
     *
     * @param config
     *            Tomcatの構成
     */
    public static void launch(final AbstractServerConfigurator config) {
        launch(EmbeddedServerFrame.class, config);
    }

    /**
     * メインウィンドウを表示する.
     *
     * @param frameClass
     *            フレームクラス、コンストラクタの第一引数はConfiguratorを取る.
     * @param config
     *            Tomcatの構成
     */
    protected static void launch(final Class<? extends JFrame> frameClass, final AbstractServerConfigurator config) {
        if (config == null) {
            throw new IllegalArgumentException();
        }

        // ロガーを設定する.
        config.initLogger();

        // クラスローダを表示 (診断用)
        ClassLoader cl = EmbeddedServerBrowserFrame.class.getClassLoader();
        int lev = 0;
        while (cl != null) {
            logger.log(Level.INFO, String.format("classLoader(%d)=%s", lev++, cl));
            cl = cl.getParent();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // システム標準のL&Fにする.
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    // L&Fを設定できなくても無視して継続する.
                }

                try {
                    // フレームクラスのコンストラクタを取得する.
                    Constructor<? extends JFrame> ctor = frameClass.getConstructor(AbstractServerConfigurator.class);

                    // フレームを表示する.
                    JFrame mainFrame = ctor.newInstance(config);
                    mainFrame.setLocationByPlatform(true);
                    mainFrame.setVisible(true);

                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
