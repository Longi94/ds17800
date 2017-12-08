package nl.vu.ds17800.core.networking;

import java.util.AbstractMap;

public class Endpoint extends AbstractMap.SimpleImmutableEntry<String, Integer>{
    public Endpoint(String s, Integer integer) {
        super(s, integer);
    }
    public String getHost() {
        return this.getKey();
    }
    public int getPort() {
        return this.getValue();
    }
    public String toString() {
        return this.getHost() + ":" + this.getValue();
    }
}
