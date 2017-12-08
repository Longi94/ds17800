package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.IncomingHandler;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Handle communication with a client
 */
public class ClientWorker implements Runnable, IClientConnection {

    // socket
    private final Socket socket;

    // handler for socket events
    private final ServerController serverController;

    // output stream to send objects to a client
    private ObjectOutputStream output;

    // input stream send objects to a client
    private ObjectInputStream input;

    // this is false until the client has a unit and the battlefield ready. before that we
    // don't want to send her any updates
    private boolean isInitialized = false;

    public ClientWorker(Socket socket, ServerController serverController) {
        this.socket = socket;
        this.serverController = serverController;
    }

    @Override
    public void run() {
        try {
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Client socket input/output failed!");
            System.exit(-1);
        }

        serverController.addClient(this);

        while(true) {
            Message m = null;
            try {
                // If we don't hear from a client in 5 seconds, the socket will be closed
                socket.setSoTimeout(5000);
            } catch (SocketException e) {
                serverController.removeClient(this);
                return;
           }

            try {
                // We only accept Message object to be sent to us
                m = (Message) input.readObject();
            }catch(SocketTimeoutException e) {
                // If we don't hear from a client in 5 seconds, the socket will be closed
                System.err.println("Timeout!");
                serverController.removeClient(this);
                return;
            } catch (IOException e) {
                // If a client closes the socket we end here
                System.err.println("Client disconnected!");
                serverController.removeClient(this);
                return;
            } catch (ClassNotFoundException e) {
                System.err.println("Unable to cast incoming Object to Message");
                System.exit(-1);
            }

            if ((MessageRequest) m.get("request") == MessageRequest.ping ) {
                // dont bother ServerController with stupid ping messages,
                // just respond and close the connection
                try {
                    output.writeObject(Message.pong());
                    socket.close();
                    serverController.removeClient(this);
                } catch (IOException e) {
                    System.out.println("unhandled..");
                    System.exit(-1); //break;
                }
            }


            // we use the time of receiving the message to be fair. Clients can have crazy
            // clocks and bad latency which will penalize them rather than other players
            m.put("timestamp", System.currentTimeMillis());

            serverController.handleMessage(m, this);
        }
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
        try {
            output.writeObject(m);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unhandled: Send message failed!");
            System.exit(-1);
        }
    }
}
