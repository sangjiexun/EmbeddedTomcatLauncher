package jp.seraphyware.embeddedtomcat.db;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource40;

/**
 * Apache Derbyのデータベースの起動・停止と、ロギングを管理する.
 */
public final class DerbyManager {

    /**
     * Derbyの出力をロガーに送るためのもの
     */
    private static final Logger logger = Logger.getLogger(DerbyManager.class.getName());

    /**
     * シングルトン
     */
    private static final DerbyManager inst = new DerbyManager();

    /**
     * Derbyデータベースの位置
     */
    private String dbPath;

    /**
     * データソース
     */
    private EmbeddedDataSource40 ds;

    /**
     * プライベートコンストラクタ
     */
    private DerbyManager() {
        super();
    }

    /**
     * このクラスの唯一のインスタンスを取得する.<br>
     * (ただしクラスローダが異なれば、それぞれ独立したクラスオブジェクトになることに注意.)<br>
     * @return インスタンス
     */
    public static DerbyManager getInstance() {
        return inst;
    }

    /**
     * データソースを取得する.<br>
     * まだ開始されていないか、停止済みの場合はIllegalStateException例外となる.<br>
     * @return
     */
    public DataSource getDataSource() {
        if (ds == null) {
            throw new IllegalStateException("uninitialized");
        }
        return ds;
    }

    /**
     * 指定したフォルダ上のDerbyデータベースを開始する.<br>
     * まだ存在しない場合は作成される.<br>
     * @param dbPath データベースフォルダの位置
     * @throws SQLException
     */
    public void start(String dbPath) throws SQLException {
        if (ds != null) {
            throw new IllegalStateException("already initialized.");
        }

        // apache derbyのログ出力先を設定する.
        System.setProperty(
                "derby.stream.error.method",
                getClass().getName() + ".getDerbyLogWriter");

        // データソースの設定
        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setUser("app");
        ds.setCreateDatabase("create");
        ds.setDatabaseName(dbPath);

        // テスト接続
        ds.getConnection().close();

        this.dbPath = dbPath;
        this.ds = ds;
    }

    /**
     * Derbyを停止する.
     */
    public void stop() {
        if (ds == null) {
            return;
        }

        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setUser("app");
        ds.setShutdownDatabase("shutdown");
        ds.setDatabaseName(dbPath);

        try {
            ds.getConnection().close();

        } catch (SQLException ex) {
            // 何もしない.
        }

        this.ds = null;
    }

    /**
     * Derbyからのログを受け取り、JDKログに転送するためのメソッド.<br>
     * @return JDKログに転送するライタ
     */
    public static Writer getDerbyLogWriter() {
        return new Writer() {

            StringBuilder buf = new StringBuilder();

            @Override
            public void close() throws IOException {
                flush();
            }

            @Override
            public void flush() throws IOException {
                synchronized (buf) {
                    if (buf.length() > 0) {
                        logger.log(Level.INFO, buf.toString());
                        buf.delete(0, buf.length());
                    }
                }
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                synchronized (buf) {
                    for (int idx = 0; idx < len; idx++) {
                        char c = cbuf[off + idx];
                        if (c == '\r' || c == '\n') {
                            flush();
                        } else {
                            buf.append(c);
                        }
                    }
                }
            }
        };
    }
}