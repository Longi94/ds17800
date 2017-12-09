package nl.vu.ds17800.core.networking;

import java.io.IOException;

public interface IMessageSendable {
    void sendMessage(Message m) throws IOException;
}
