package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.IncomingMessage;

import java.net.Socket;

public class ClientWorker extends AbstractWorker implements IClientConnection {

    // this is false until the client has a unit and the battlefield ready. before that we
    // don't want to send her any updates
    private boolean isInitialized = false;

    // handler for socket events
    private final ServerController serverController;

    public ClientWorker(Socket socket, ServerController serverController) {
        super(socket);
        this.serverController = serverController;
    }

    @Override
    protected void handleMessage(IncomingMessage inm) {
        // we use the time of receiving the message to be fair. Clients can have crazy
        // clocks and bad latency which will penalize them rather than other players
        inm.getMessage().put("timestamp", System.currentTimeMillis());
        serverController.handleMessage(inm);
    }

    /**
     * Register this client so we can send messages to it
     */
    @Override
    protected void register() {
        serverController.addClient(this);
    }

    /**
     * Remove this client from
     */
    @Override
    protected void unregister() {
        serverController.removeClient(this);
    }


    @Override
    public void sendMessage(Message m) {
        if(!isInitialized && (MessageRequest)m.get("request") != MessageRequest.clientConnect) {
            // we skip messages until the client is initialized. unless we're actually sending
            // out the initialization message right now.
            System.out.println("Client not initialized, skipping!");
            return;
        } else {
            this.isInitialized = true;
        }
        super.sendMessage(m);
    }
}
