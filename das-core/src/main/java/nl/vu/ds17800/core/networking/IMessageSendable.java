package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

public interface IMessageSendable {
    void sendMessage(Message m);
}
