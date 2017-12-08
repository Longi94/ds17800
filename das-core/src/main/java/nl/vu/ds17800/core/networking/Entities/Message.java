package nl.vu.ds17800.core.networking.Entities;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.RequestStage;
import nl.vu.ds17800.core.model.units.Unit;

import java.io.Serializable;
import java.util.HashMap;

import static nl.vu.ds17800.core.model.MessageRequest.acknowledge;

public class Message extends HashMap<String, Object> implements Serializable, Comparable<Message> {

    /**
     * Message class is a regular HashMap with String key and Object value;
     */
    public Message() {
        super();
        this.put("timestamp", System.currentTimeMillis());
    }

    /**
     * Copy constructor, makes a shallow copy
     * @param m make copy of
     */
    public Message(Message m) {
        super(m);
    }

    @Override
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

    @Override
    public int compareTo(Message o) {
        // messages are ordered by their timestamp, we use this to order message queues
        long t1 = (long)this.get("timestamp");
        long t2 = (long)o.get("timestamp");
        return (int)(t1 - t2);
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

    public static Message ping() {
        Message m = new Message();
        m.put("request", MessageRequest.ping);
        return m;
    }

    public static Message pong() {
        Message m = new Message();
        m.put("request", MessageRequest.pong);
        return m;
    }

    public static Message dealDamage(int x, int y, int attackPoints) {
        Message m = new Message();
        m.put("request", MessageRequest.dealDamage);
        m.put("x", x);
        m.put("y", y);
        m.put("damage", attackPoints);
        return m;
    }

    public static Message clientDisconnect(String unitId) {
        Message m = new Message();
        m.put("request", MessageRequest.clientDisconnect);
        m.put("id", unitId);
        return m;
    }

    public static Message nop() {
        Message m = new Message();
        m.put("request", MessageRequest.nop);
        return m;
    }

    public static Message moveUnit(Unit unit, int x, int y) {
        Message m = new Message();
        m.put("request", MessageRequest.moveUnit);
        m.put("unit", unit);
        m.put("x", x);
        m.put("y", y);
        return m;
    }

    public static Message healDamage(int x, int y, int attackPoints) {
        Message m = new Message();
        m.put("request", MessageRequest.healDamage);
        m.put("x", x);
        m.put("y", y);
        m.put("healed", attackPoints);
        return m;
    }

    public static Message toRequestStage(Message m, RequestStage rs) {
        Message msg = new Message(m);
        msg.put("requestStage", rs);
        return msg;
    }

    public static Message ask(Message m) {
        return Message.toRequestStage(m, RequestStage.ask);
    }

    public static Message reject(Message m) {
        return Message.toRequestStage(m, RequestStage.reject);
    }

    public static Message accept(Message m) {
        return Message.toRequestStage(m, RequestStage.accept);
    }

    public static Message commit(Message m) {
        return Message.toRequestStage(m, RequestStage.commit);
    }
}
