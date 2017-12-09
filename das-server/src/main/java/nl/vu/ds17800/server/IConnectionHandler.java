package nl.vu.ds17800.server;

import java.net.Socket;

public interface IConnectionHandler {
    public void handle(Socket s);
}
