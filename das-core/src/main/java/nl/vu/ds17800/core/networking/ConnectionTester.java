package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Thread for heartbeating implementation
 */
public class ConnectionTester extends Thread {
    private Map<String, PoolEntity> socketPool;
    private final String HEARTBEATING = "heartbeating";
    private IncomingHandler incomingHandler;

    public ConnectionTester(Map<String, PoolEntity> pool, IncomingHandler handler){
        incomingHandler = handler;
        socketPool = pool;
    }

    public void run(){
        while(true){
            synchronized (socketPool){
                Set<String> keys = socketPool.keySet();
                for(String key : keys){
                    PoolEntity entity = socketPool.get(key);
                    ObjectOutputStream oout = null;
                    ObjectInputStream oin = null;
                    Message testMessage = new Message();
                    testMessage.put("__communicationType", HEARTBEATING);
                    try {
                        oout = new ObjectOutputStream(entity.socket.getOutputStream());
                        oout.writeObject(testMessage);
                    } catch (IOException e) {
                        // socket is lost
                        incomingHandler.connectionLost(entity.socket.getInetAddress().toString(), entity.socket.getPort());
                        socketPool.remove(key);
                    }
                }
            }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
}
