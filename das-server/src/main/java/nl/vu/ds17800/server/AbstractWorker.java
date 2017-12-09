package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.IMessageSendable;
import nl.vu.ds17800.core.networking.IncomingMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Handle communication with a remote node (client or server)
 */
public abstract class AbstractWorker implements Runnable, IMessageSendable {

    // socket
    private final Socket socket;

    // output stream to send objects to a remote
    private ObjectOutputStream output;

    // input stream send objects to a remote
    private ObjectInputStream input;

    public AbstractWorker(Socket socket) {
        this.socket = socket;

        try {
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Socket IO failed!");
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        register();

        Message m = null;
        while(true) {
            try {
                // If we don't hear from a client in 5 seconds, the socket will be closed
                socket.setSoTimeout(5000);
            } catch (SocketException e) {
                break;
           }

            try {
                // We only accept Message object to be sent to us
                m = (Message) input.readObject();
            } catch(SocketTimeoutException e) {
                // If we don't hear from a client in 5 seconds, the socket will be closed
                System.err.println("Timeout!");
                break;
            } catch (IOException e) {
                // If a client closes the socket we end here
                System.err.println("Remote disconnected!");
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("Unable to cast incoming Object to Message");
                System.exit(-1);
            }

            if ((MessageRequest) m.get("request") == MessageRequest.ping ) {
                // don't bother ServerController with silly ping messages,
                // just respond and close the connection
                try {
                    output.writeObject(Message.pong());
                } catch (IOException e) {
                    System.err.println("Pong could not be delivered..");
                    break;
                }
            }

            if ((MessageRequest) m.get("request") == MessageRequest.nop ) {
                // the remote node just sent a nop, to keep connection alive
                continue;
            }

            handleMessage(new IncomingMessage(m, this));
        }

        unregister();
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    abstract protected void handleMessage(IncomingMessage inm);
    abstract protected void register();
    abstract protected void unregister();

    @Override
    public void sendMessage(Message m) {
        try {
            output.writeObject(m);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unhandled: Send message failed!");
            System.exit(-1);
        }
    }
}
