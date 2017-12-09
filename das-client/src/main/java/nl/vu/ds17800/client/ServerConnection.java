package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.Endpoint;
import nl.vu.ds17800.core.networking.Message;
import nl.vu.ds17800.core.networking.IMessageSendable;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.IncomingMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client-Server socket connection instance
 */
public class ServerConnection implements IMessageSendable {
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

        System.out.println("SEND> " +message);
        output.writeObject(message);

        // blocking until we get response
        Message response = (Message)input.readObject();
        System.out.println("RECV> " + response);

        incomingHandler.handleMessage(new IncomingMessage(response, this));
    }

    @Override
    public void sendMessage(Message m) throws IOException {
        System.out.println("SEND> " +m);

        output.writeObject(m);
    }

    public void close() {
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startListenerThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (server.isConnected() && !server.isClosed()) {
                    try {
                        Message m = (Message)input.readObject();
                        // messages in the buffer
                        System.out.println("RECV> " + m);
                        incomingHandler.handleMessage(new IncomingMessage(m, null));
                    } catch (Exception e) {
                        System.err.println("unhandled: Problem receiving! wtf?!");
                        close();
                    }
                }
                System.out.println("No more listening! :)");
            }
        }).start();
    }
}
