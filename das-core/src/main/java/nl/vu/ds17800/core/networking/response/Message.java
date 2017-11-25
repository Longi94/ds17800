package nl.vu.ds17800.core.networking.response;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class Message extends HashMap<String, Object> implements Serializable {
    public Message() {
        super();

        this.put("timestamp", System.currentTimeMillis());
    }
}
