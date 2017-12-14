package nl.vu.ds17800.server;

import nl.vu.ds17800.core.networking.Message;

public interface IOnTimeout {
    void messageTimeout(Message message);
}
