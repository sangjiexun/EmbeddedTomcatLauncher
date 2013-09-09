package jp.seraphyware.embeddedtomcat.data;

public final class UniqueKey {

    private String data;

    public UniqueKey() {
        this("");
    }

    public UniqueKey(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }
}
