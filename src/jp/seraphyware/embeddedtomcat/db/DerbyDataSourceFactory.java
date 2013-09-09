package jp.seraphyware.embeddedtomcat.db;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

/**
 * グローバルネーミングリソースに設定するDerbyのデータソースのファクトリ
 */
public class DerbyDataSourceFactory implements ObjectFactory {

    /**
     * Apache Derbyのデータベースの起動・停止と、ロギングを管理するクラス.
     */
    private static final DerbyManager derbyManager = DerbyManager.getInstance();

    @Override
    public Object getObjectInstance(
            Object obj,
            Name name,
            Context nameCtx,
            Hashtable<?, ?> environment
            ) throws Exception {
        return derbyManager.getDataSource();
    }
}
