package nl.vu.ds17800.server;

import nl.vu.ds17800.core.networking.IncomingMessage;

import java.net.Socket;

/**
 * Handle outgoing messages to a remote server, and responses to those messages
 */
public class ServerOutWorker extends AbstractWorker implements IServerConnection {

    // handler for socket events
    private final ServerController serverController;

    public ServerOutWorker(Socket socket, ServerController serverController) {
        super(socket);
        this.serverController = serverController;
        this.name = "serverOUT";
    }

    @Override
    protected void handleMessage(IncomingMessage inm) {
        serverController.handleMessage(inm);
    }

    @Override
    protected void onConnect() {
        serverController.addServer(this);
    }

    @Override
    protected void onDisconnect() {
        serverController.removeServer(this);
    }
}
