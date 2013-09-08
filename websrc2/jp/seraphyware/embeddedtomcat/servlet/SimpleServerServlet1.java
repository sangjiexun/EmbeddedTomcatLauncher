package jp.seraphyware.embeddedtomcat.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * JNDIを確認するためのサーブレット実装例.<br>
 */
public class SimpleServerServlet1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(SimpleServerServlet1.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = resp.getWriter();
        pw.println("<html><head><title>");
        pw.println(getClass().getSimpleName());
        pw.println("</title></head><body><h1>");
        pw.println(getClass().getName());
        pw.println("</h1>");

        try {

            // クラスローダの表示
            try {
                ClassLoader cl = getClass().getClassLoader();
                pw.print("<ol>");
                while (cl != null) {
                    pw.print("<li>");
                    pw.println(cl);
                    cl = cl.getParent();
                }
                pw.print("</ol>");

            } catch (Exception ex) {
                ex.printStackTrace(pw);
            }

            // データソースをJNDI経由で取得したもの
            ArrayList<DataSource> dss = new ArrayList<DataSource>();

            // JNDI経由でリソース等にアクセス
            Context initCtx = new InitialContext();
            try {
                // JNDIを用いて間接的にデータソースを参照する.
                DataSource ds1 = (DataSource) initCtx.lookup("java:comp/env/jdbc/ds");
                dss.add(ds1);

                // JNDI経由の環境変数
                Integer testvalue2 = (Integer) initCtx.lookup("java:comp/env/testvalue2");

                pw.println("<p>testvlaue2=" + testvalue2 + "</p>");

            } catch (Exception ex) {
                ex.printStackTrace(pw);

            } finally {
                initCtx.close();
            }

            // データソースを経由したderbyデータベースへのアクセステスト
            String strSQL = "values (CURRENT TIMESTAMP, CURRENT_USER)";
            for (DataSource ds : dss) {
                pw.println("<hr>");
                pw.println("<p>ds=" + ds + "</p>");
                pw.println("<p>dsClass=" + ds.getClass() + "</p>");
                pw.println("<p>sql=" + strSQL + "</p>");

                try (Connection conn = ds.getConnection();
                        Statement stm = conn.createStatement();
                        ResultSet rs = stm.executeQuery(strSQL)) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp(1);
                        pw.println("<p>timestamp=" + ts + "</p>");
                        pw.println("<p>user=" + rs.getString(2) + "</p>");
                    }
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.toString(), ex);
            ex.printStackTrace(pw);
        }

        pw.println("</body></html>");
    }
}