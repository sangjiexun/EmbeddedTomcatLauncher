package jp.seraphyware.embeddedtomcat.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 動的に設定するテスト用フィルタ.<br>
 */
public class MyServletFilter implements Filter {

    /**
     * ロガー
     */
    private static final Logger logger = Logger.getLogger(MyServletFilter.class.getName());

    /**
     * フィルタの初期化
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.info("MyServletFilter#init()");

        // フィルタのパラメータの表示
        Enumeration<String> enm = config.getInitParameterNames();
        while (enm.hasMoreElements()) {
            String name = enm.nextElement();
            String val = config.getInitParameter(name);
            logger.log(Level.INFO, "filterParameter " + name + "=" + val);
        }
    }

    /**
     * フィルタの破棄
     */
    @Override
    public void destroy() {
        logger.info("MyServletFilter#destroy()");
    }

    /**
     * フィルタ処理
     */
    @Override
    public void doFilter(ServletRequest req0, ServletResponse res0, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) req0;
        HttpServletResponse res = (HttpServletResponse) res0;

        // リクエストURIの確認
        String requestURI = req.getRequestURI();
        logger.info("MyServletFilter#doFilter reqURI=" + requestURI);

        // プログラム的に設定したサーブレットコンテキストとリクエスト属性の確認
        String greeting = (String) req.getServletContext().getAttribute("greeting");
        String random = (String) req.getAttribute("random");
        logger.info("△" + greeting + "/" + random);

        chain.doFilter(req, res);
    }
}
