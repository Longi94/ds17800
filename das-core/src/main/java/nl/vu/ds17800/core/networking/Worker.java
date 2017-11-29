package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Worker extends Thread{
    private IncomingHandler messageshandler;
    private PoolEntity connectionEntity;

    Worker(IncomingHandler handler, PoolEntity entity){
        messageshandler = handler;
        connectionEntity = entity;
    }

    public void run(){
        ObjectInputStream oin = null;
        ObjectOutputStream oout = null;
        try {
            oin = new ObjectInputStream(connectionEntity.socket.getInputStream());
            oout = new ObjectOutputStream(connectionEntity.socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        while(true){
            try {
                Message message = (Message)oin.readObject();
                synchronized (connectionEntity.responseBuffer){
                    if(message.get("__communicationType").equals("_response")){
                        connectionEntity.responseBuffer.add(message);
                        connectionEntity.responseBuffer.notifyAll();
                        continue;
                    }
                }
                message = messageshandler.handleMessage(message);
                message.put("__communicationType", "__response");
                oout.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}