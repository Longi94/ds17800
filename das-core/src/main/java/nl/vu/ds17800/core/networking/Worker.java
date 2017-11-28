package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Worker extends Thread{
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