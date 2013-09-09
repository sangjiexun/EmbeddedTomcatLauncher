package jp.seraphyware.embeddedtomcat.data;

import java.security.SecureRandom;

import javax.xml.bind.DatatypeConverter;

/**
 * ランダムキーを生成するファクトリクラス.<br>
 */
public class UniqueKeyFactory {

    /**
     * ランダム値を生成するためのジェネレータ.<br>
     * (現状、一応、SecureRandomのnextBytesはsynchronizedされているのでスレッドセーフといえる.)<br>
     * http://stackoverflow.com/questions/1461568/is-securerandom-thread-safe
     */
    private SecureRandom rng = new SecureRandom();

    /**
     * ランダムキーを生成して返す.
     * @return ランダムキー
     */
    public UniqueKey create() {
        // ランダム値を生成する.
        byte[] r = new byte[20];
        rng.nextBytes(r);
        return new UniqueKey(DatatypeConverter.printHexBinary(r));
    }
}
