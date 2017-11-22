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
    private LinkedList<MessageContainer> incomeBuffer;
    private HashMap<String, Socket> socketPool;

    public CommunicationImpl(IncomingHandler handler){
        incomeBuffer = new LinkedList<MessageContainer>();
        incomeHandler = handler;
        socketPool = new HashMap<String, Socket>();
    }

    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException {
        Socket socket;
        socket = socketPool.get(inetAddress);
        if(socket == null)
            socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
        Message testMessage = new Message();
        testMessage.type = HEARTBEATING;
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
        NetworkRouter routerThread = new NetworkRouter(incomeBuffer);
        Worker worker = new Worker(incomeHandler, incomeBuffer);
        routerThread.start();
        worker.start();
    }

    private class NetworkRouter extends Thread{
        private LinkedList<MessageContainer> messagesBuffer;
        public NetworkRouter(LinkedList<MessageContainer> list){
            this.messagesBuffer = list;
        }

        public void run(){
            ServerSocket ssocket = null;
            try {
                ssocket = new ServerSocket(DSPORT);
                while(true){
                    Socket socket = ssocket.accept();
                    ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) oin.readObject();
                    if(message.type == HEARTBEATING)
                        continue;
                    MessageContainer mContainer = new MessageContainer();
                    mContainer.socket = socket;
                    mContainer.message = message;
                    synchronized (messagesBuffer){
                        this.messagesBuffer.add(mContainer);
                        this.messagesBuffer.notifyAll();
                    }
                }
            } catch (IOException e) {
            } catch (ClassNotFoundException e) {
            }
        }
    }

    private class Worker extends Thread{
        private IncomingHandler messageshandler;
        private LinkedList<MessageContainer> messagesBuffer;

        Worker(IncomingHandler handler, LinkedList<MessageContainer> list){
            this.messagesBuffer = list;
            messageshandler = handler;
        }

        public void run(){
            while(true){
                MessageContainer mContainer = null;
                try{
                    synchronized (messagesBuffer){
                        while(mContainer == null){
                            mContainer = messagesBuffer.getFirst();
                            messagesBuffer.wait();
                        }
                        messagesBuffer.removeFirst();
                    }
                    ObjectOutputStream oout = new ObjectOutputStream(mContainer.socket.getOutputStream());
                    oout.writeObject(messageshandler.handleMessage(mContainer.message));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }
}
