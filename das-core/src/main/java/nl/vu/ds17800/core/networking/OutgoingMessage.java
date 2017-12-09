package nl.vu.ds17800.core.networking;

import java.io.IOException;

public class OutgoingMessage implements Comparable<OutgoingMessage> {
    private final IMessageSendable recipient;
    private final Message message;

    public OutgoingMessage(IMessageSendable recipient, Message message) {
        this.recipient = recipient;
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public long getTimestamp() {
        return (long) message.get("timestamp");
    }

    public void send() throws IOException {
        recipient.sendMessage(message);
    }

    @Override
    public int compareTo(OutgoingMessage o) {
        return this.message.compareTo(o.getMessage());
    }
}
