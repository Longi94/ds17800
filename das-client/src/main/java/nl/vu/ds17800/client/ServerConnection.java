package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.Endpoint;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.IMessageSendible;
import nl.vu.ds17800.core.networking.IncomingHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client-Server socket connection instance
 */
public class ServerConnection implements IMessageSendible {
    private final IncomingHandler incomingHandler;
    private final Endpoint serverEndpoint;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket server;

    public ServerConnection(Endpoint serverEndpoint, IncomingHandler handler) {
        this.serverEndpoint = serverEndpoint;
        this.incomingHandler = handler;
    }

    public void listenSocket() throws IOException {
        server = new Socket(serverEndpoint.getHost(), serverEndpoint.getPort());
        output = new ObjectOutputStream(server.getOutputStream());
        input = new ObjectInputStream(server.getInputStream());
    }

    public void initializeClientUnit(String unitType, String unitId) throws IOException, ClassNotFoundException {
        Message message = new Message();
        message.put("request", MessageRequest.clientConnect);
        if (unitId != null) {
            message.put("id", unitId);
        } else {
            message.put("type", unitType);
        }

        output.writeObject(message);
        Message response = (Message)input.readObject();

        incomingHandler.handleMessage(response);
    }

    public void consumeIncomingMessages() throws IOException, ClassNotFoundException {
        Message m;
        while (input.available() > 0) {
            // messages in the buffer
            incomingHandler.handleMessage((Message)input.readObject());
        }
    }

    @Override
    public void sendMessage(Message m) {
        try {
            output.writeObject(m);
        } catch (IOException e) {
            System.out.println("unable to write message to socket!");
        }
    }
}
