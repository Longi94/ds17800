package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import nl.vu.ds17800.core.networking.response.MessageContainer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by hacku on 11/21/17.
 */
public class CommunicationImpl implements Communication {
    static private final int DSPORT = 666;
    static private final String HEARTBEATING = "heartbeating";
    private IncomingHandler incomeHandler;
    private HashMap<String, Socket> socketPool;

    public CommunicationImpl(IncomingHandler handler){
        incomeHandler = handler;
        socketPool = new HashMap<String, Socket>();
    }

    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException {
        Socket socket;
        socket = socketPool.get(inetAddress);
        if(socket == null)
            socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
        Message testMessage = new Message();
        testMessage.put("type", HEARTBEATING);
        ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
        try{
            oout.writeObject(testMessage);
        } catch (IOException e){
            socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());
        }
        socketPool.put(inetAddress, socket);
        oout.writeObject(message);
        return (Message)oin.readObject();
    }

    public List<Map<String, Object>> getServers(){
        return null;
    }


    public boolean registerServer() {
        return false;
    }

    public void init(){
        NetworkRouter routerThread = new NetworkRouter(incomeHandler);
        routerThread.start();
    }

    private class NetworkRouter extends Thread{
        private IncomingHandler messageHandler;
        public NetworkRouter(IncomingHandler incomingHandler){
            super();
            messageHandler = incomingHandler;
        }

        public void run(){
            ServerSocket ssocket = null;
            try {
                ssocket = new ServerSocket(DSPORT);
                while(true){
                    Socket socket = ssocket.accept();
                    socketPool.put(socket.getInetAddress().toString(), socket);
                    Worker worker = new Worker(messageHandler, socket);
                    worker.start();
                }
            } catch (IOException e) {
            }
        }
    }

    private class Worker extends Thread{
        private IncomingHandler messageshandler;
        private Socket connection;

        Worker(IncomingHandler handler, Socket socket){
            messageshandler = handler;
            connection = socket;
        }

        public void run(){
            ObjectInputStream oin = null;
            ObjectOutputStream oout = null;
            try {
                oin = new ObjectInputStream(connection.getInputStream());
                oout = new ObjectOutputStream(connection.getOutputStream());
            } catch (IOException e) {
             return;
            }
            while(true){
                try {
                    Message message = (Message)oin.readObject();
                    oout.writeObject(messageshandler.handleMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
