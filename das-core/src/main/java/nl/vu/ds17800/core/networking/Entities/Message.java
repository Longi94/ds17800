package nl.vu.ds17800.core.networking.Entities;

import java.io.Serializable;
import java.util.HashMap;

import static nl.vu.ds17800.core.model.MessageRequest.acknowledge;

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
    public Message(Message m) {
        super(m);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String k : this.keySet()) {
            if(k.equals("battlefield")) {
                // too long to print
                sb.append("  " + k + ": <battlefield>");
            } else {
                sb.append("  " + k + ": " + this.get(k));
            }
        }
        return sb.toString();
    }

    /**
     * convenience constructor for creating an ack Message
     * @param message
     * @return
     */
    public static Message ack(Message message) {
        Message m = new Message();
        m.put("request", acknowledge);
        return m;
    }
}
