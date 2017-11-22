package nl.vu.ds17800.core.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by hacku on 11/21/17.
 */
public class CommunicationImpl implements Communication {
    static private final int DSPORT = 666;
    private IncomingHandler incomeHandler;

    public CommunicationImpl(IncomingHandler handler){
        incomeHandler = handler;
    }

    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException {
        Socket socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
        ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
        oout.writeObject(message);
        return (Message)oin.readObject();

    }

    public void init(){
        NetworkRouter routerThread = new NetworkRouter(incomeHandler);
        routerThread.start();
    }

    private class NetworkRouter extends Thread{
        private IncomingHandler messagesHandler;

        public NetworkRouter(IncomingHandler handler){
            messagesHandler = handler;
        }

        public void run(){
            ServerSocket ssocket = null;
            try {
                ssocket = new ServerSocket(DSPORT);
                while(true){
                    Socket socket = ssocket.accept();
                    ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
                    Message message = (Message) oin.readObject();
                    oout.writeObject(messagesHandler.handleMessage(message));
                    socket.close();
                }
            } catch (IOException e) {
            } catch (ClassNotFoundException e) {
            }
        }
    }
}
