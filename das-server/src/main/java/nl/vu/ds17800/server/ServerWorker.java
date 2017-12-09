package nl.vu.ds17800.server;

import nl.vu.ds17800.core.networking.IncomingMessage;

import java.net.Socket;

public class ServerWorker extends AbstractWorker implements IServerConnection {

    // handler for socket events
    private final ServerController serverController;

    public ServerWorker(Socket socket, ServerController serverController) {
        super(socket);
        this.serverController = serverController;
    }

    @Override
    protected void handleMessage(IncomingMessage inm) {
        serverController.handleMessage(inm);
    }

    @Override
    protected void register() {
        serverController.addServer(this);

    }

    @Override
    protected void unregister() {
        serverController.removeServer(this);
    }
}
