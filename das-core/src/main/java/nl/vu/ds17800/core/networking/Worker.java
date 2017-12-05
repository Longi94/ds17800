package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Class for implementation listener of specific Socket
 * it always listens the socket and either pushes Entities message to responseBuffer or just answers with message using
 * the same __communicationID and Entities __communicationType
 */
public class Worker extends Thread{
    private final String    HEARTBEATING = "heartbeating";
    private IncomingHandler messageshandler;
    private PoolEntity connectionEntity;

    Worker(IncomingHandler handler, PoolEntity entity){
        messageshandler = handler;
        connectionEntity = entity;
    }

    public void run(){
        ObjectInputStream oin = null;
        ObjectOutputStream oout = null;
        System.out.println("New connection " + connectionEntity.socket.getInetAddress() + " on port " + connectionEntity.socket.getPort());
        try {
            oin = new ObjectInputStream(connectionEntity.socket.getInputStream());
            oout = new ObjectOutputStream(connectionEntity.socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        while(true){
            try {
                Message message = (Message)oin.readObject();
                if(((String)message.get("__communicationType")).equals(HEARTBEATING))
                    continue;

                System.out.println("Incoming message: " + message);
                if(((String)message.get("__communicationType")).equals("__response")){
                    synchronized (connectionEntity.responseBuffer){
                        connectionEntity.responseBuffer.add(message);
                        connectionEntity.responseBuffer.notifyAll();
                    }
                    continue;
                }
                String messageKey = (String)message.get("__communicationID");
                message = messageshandler.handleMessage(message);
                if(message == null) {
                    System.out.println("No response!");
                } else {
                    System.out.println("Response message to send: " + message);
                }

                if(message == null)
                    continue;

                message.put("__communicationID", messageKey);
                message.put("__communicationType", "__response");
                System.out.println("Sending response! " + message);
                oout.writeObject(message);
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
                return;
            }
        }
    }
}