package nl.vu.ds17800.server;

import javax.print.attribute.standard.MediaSize;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Listen to incoming client connections
 */
public class ClientListener {

    private final ServerController serverController;
    private ServerSocket socket;

    ClientListener(ServerController sc) {
        this.serverController = sc;
    }

    void listenSocket(int port) {
        try {
            this.socket = new ServerSocket(port);
        } catch(IOException e) {
            System.out.println("Can't open socket on port " + port);
            System.exit(-1);
        }

        while(true) {
            ClientWorker w;

            try {
                // new client connection! Give it to our controller to handle its incoming messages
                w = new ClientWorker(socket.accept(), serverController);

                // start accepting messages
                Thread t = new Thread(w);
                t.start();
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
