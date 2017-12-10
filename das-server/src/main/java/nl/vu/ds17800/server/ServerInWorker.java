package nl.vu.ds17800.server;

import nl.vu.ds17800.core.networking.IncomingMessage;

import java.net.Socket;

/**
 * Handle outgoing messages to a remote server
 */
public class ServerInWorker extends AbstractWorker implements IServerConnection {

    // handler for socket events
    private final ServerController serverController;

    public ServerInWorker(Socket socket, ServerController serverController) {
        super(socket);
        this.serverController = serverController;
        this.name = "serverIN";
    }

    @Override
    protected void handleMessage(IncomingMessage inm) {
        serverController.handleMessage(inm);
    }

    @Override
    protected void onConnect() {
        System.out.println("Connected to server peer!");
    }

    @Override
    protected void onDisconnect() {
        System.out.println("Disconnected from server peer!");
    }
}
