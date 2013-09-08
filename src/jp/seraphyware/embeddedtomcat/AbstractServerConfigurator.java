package jp.seraphyware.embeddedtomcat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.servlet.ServletException;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

/**
 * Tomcatを構成するための抽象ベースクラス.<br>
 * ログの設定やベースフォルダの取得などの共通処理を行う.<br>
 * <br>
 * TomcatはデフォルトではJDKのロガーを用いているので、JDKのロガーを設定することで
 * Tomcatのサーバーログをコントロールすることができる.<br>
 */
public abstract class AbstractServerConfigurator {

    /**
     * デフォルトポート.<br>
     */
    protected int port = 8081;

    /**
     * Tomcatインスタンス.<br>
     * 派生クラスで初期化する.<br>
     */
    protected Tomcat tomcat;

    /**
     * ウェブアプリケーションコンテキスト.<br>
     * 派生クラスで初期化する.<br>
     */
    protected StandardContext ctx;

    /**
     * 保護されたコンストラクタ.<br>
     */
    protected AbstractServerConfigurator() {
        // リスンするポートをシステムプロパティより取得する.
        String strPort = System.getProperty("port");
        if (strPort != null && strPort.trim().length() > 0) {
            port = Integer.parseInt(strPort);
        }
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public StandardContext getContext() {
        return ctx;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Tomcatを構成する.
     *
     * @throws IOException
     *             失敗
     * @throws ServletException
     *             失敗
     */
    public abstract void init() throws IOException, ServletException;

    /**
     * このクラスがあるディレクトリ(binフォルダ)またはjarファイルの親フォルダを返す.<br>
     * 不明な場合は「カレントディレクトリ」を返す.<br>
     *
     * @return 親ディレクトリ
     */
    protected static File getAppRootDir() {
        ProtectionDomain pd = AbstractServerConfigurator.class.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        if (cs == null) {
            // java.*, javax.*などのブートストラップクラスローダによる場合は
            // これはnullとなる.自前のクラスはnullにはならない(はず).
            return new File(".");
        }
        try {
            // クラスをロードしたクラスパスの入ったURLを取得する.
            // binフォルダまたはjarファイルを指すので、
            // その親をベースとして返す.
            URL url = cs.getLocation();
            return new File(url.toURI()).getParentFile();

        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * ログディレクトリを取得する.
     *
     * @return ログディレクトリ
     * @throws IOException
     *             失敗
     */
    public File getLogsDir() {
        File logDir = new File(getAppRootDir(), "logs");
        logDir.mkdirs();
        return logDir;
    }

    /**
     * JDKのロガーを初期化する.
     */
    public void initLogger() {
        // 明示的にファイル出力のロガーを設定
        File logDir = getLogsDir();

        // ログファイル名の設定
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String logName = fmt.format(new Date()) + ".log";
        String logFile = new File(logDir, logName).getAbsolutePath();

        // 現在のロガーの設定をクリア
        LogManager logManager = LogManager.getLogManager();
        logManager.reset();

        // 独自のコンソールハンドラ(フォーマットなし)
        Handler consoleHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                String name = record.getLoggerName();
                Throwable t = record.getThrown();
                System.out.println(name + " : " + msg);
                if (t != null) {
                    t.printStackTrace(System.out);
                }
            }

            @Override
            public void flush() {
                // なにもしない
            }

            @Override
            public void close() throws SecurityException {
                // なにもしない
            }
        };

        // ルートロガーに追加
        Logger.getLogger("").addHandler(consoleHandler);

        // ファイルハンドラ作成
        try {
            Handler fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            fileHandler.setEncoding("UTF-8");

            // ルートロガーに追加
            Logger.getLogger("").addHandler(fileHandler);

        } catch (Exception ex) {
            // ファイルハンドラに作成に失敗しても続行する.
            ex.printStackTrace();
        }
    }
}
