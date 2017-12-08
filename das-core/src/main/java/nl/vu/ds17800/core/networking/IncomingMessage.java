package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

public class IncomingMessage implements Comparable<IncomingMessage> {
    private final Message message;
    private final IMessageSendable sender;

    public IncomingMessage(Message message, IMessageSendable sender) {
        this.message = message;
        this.sender = sender;
    }

    public IMessageSendable getSender() {
        return sender;
    }

    public Message getMessage() {
        return message;
    }

    public long getTimestamp() {
        return (long) message.get("timestamp");
    }

    @Override
    public int compareTo(IncomingMessage o) {
        return this.message.compareTo(o.getMessage());
    }
}
