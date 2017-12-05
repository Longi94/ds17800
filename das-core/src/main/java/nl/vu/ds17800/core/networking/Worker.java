package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

import java.io.IOException;


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
        System.out.println("New connection " + connectionEntity.socket.getInetAddress() + " on port " + connectionEntity.socket.getPort());
        while(true){
            try {
                Message message = (Message)connectionEntity.inputStream.readObject();
                if(((String)message.get(Communication.KEY_COMM_TYPE)).equals(HEARTBEATING))
                    continue;

                System.out.println("<- " + message);
                if(((String)message.get(Communication.KEY_COMM_TYPE)).equals("__response")){
                    synchronized (connectionEntity.responseBuffer){
                        connectionEntity.responseBuffer.add(message);
                        connectionEntity.responseBuffer.notifyAll();
                    }
                    continue;
                }
                String messageKey = (String)message.get(Communication.KEY_COMM_ID);
                message = messageshandler.handleMessage(message, connectionEntity);

                if(message == null) {
                    System.out.println("Nope you may not be null!");
                    System.exit(1);
                }

                message.put(Communication.KEY_COMM_ID, messageKey);
                message.put(Communication.KEY_COMM_TYPE, "__response");
                synchronized (connectionEntity.outputStream){
                    connectionEntity.outputStream.writeObject(message);
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
        }
    }
}