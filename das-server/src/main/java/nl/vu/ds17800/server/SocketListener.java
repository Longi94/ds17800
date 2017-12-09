package nl.vu.ds17800.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Listen to incoming client connections
 */
public class SocketListener {

    private final IConnectionHandler connectionHandler;
    private ServerSocket socket;

    public SocketListener(IConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    void listenSocket(int port) {
        try {
            this.socket = new ServerSocket(port);
        } catch(IOException e) {
            System.out.println("Can't open socket on port " + port);
            System.exit(-1);
        }

        while(true) {
            try {
                // new connection!
                this.connectionHandler.handle(socket.accept());
            } catch (IOException e) {
                System.out.println("Accept failed: " + port);
                System.exit(-1);
            }
        }
    }

    @Override
    protected void finalize() {
        // called when cleaning up, if a server exists for instance
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not close socket");
            System.exit(-1);
        }
    }
}
