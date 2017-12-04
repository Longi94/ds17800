package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Class for implementation listener of specific Socket
 * it always listens the socket and either pushes response message to responseBuffer or just answers with message using
 * the same __communicationID and response __communicationType
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
        System.out.println("Started listening port" + connectionEntity.socket.getInetAddress() + " on port " + connectionEntity.socket.getPort());
        try {
            oin = new ObjectInputStream(connectionEntity.socket.getInputStream());
            oout = new ObjectOutputStream(connectionEntity.socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        while(true){
            try {
                Message message = (Message)oin.readObject();
                if(message.get("__communicationType").equals(HEARTBEATING))
                    continue;

                if(message.get("__communicationType").equals("__response")){
                    synchronized (connectionEntity.responseBuffer){
                        connectionEntity.responseBuffer.add(message);
                        connectionEntity.responseBuffer.notifyAll();
                    }
                    continue;
                }
                String messageKey = message.get("__communicationID").toString();
                message = messageshandler.handleMessage(message);
                message.put("__communicationID", messageKey);
                message.put("__communicationType", "__response");
                oout.writeObject(message);
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
                return;
            }
        }
    }
}