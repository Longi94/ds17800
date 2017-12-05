package nl.vu.ds17800.core.networking.Entities;

import java.io.Serializable;
import java.util.HashMap;

public class Message extends HashMap<String, Object> implements Serializable {
    /**
     * Message class is a regular HashMap with String key and Object value;
     * Some keys are reserved for internal using:
     * __communicationType - describes which type of message it is;(possible values: __response, __request)
     * __communicationID - describe id of message; Needed for distinguishing different responses for different requests;
     */
    public Message() {
        super();
        this.put("timestamp", System.currentTimeMillis());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message:");
        for (String k : this.keySet()) {
            sb.append("  " + k + ": " + this.get(k));
        }
        return sb.toString();
    }
}
