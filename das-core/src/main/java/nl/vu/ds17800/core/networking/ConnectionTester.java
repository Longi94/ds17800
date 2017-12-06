package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Message;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Thread for heartbeating implementation
 */
public class ConnectionTester extends Thread {
    private Map<String, PoolEntity> socketPool;
    private final String HEARTBEATING = "heartbeating";
    private IncomingHandler incomingHandler;
    private int period = 2000;

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
                    Message testMessage = new Message();
                    testMessage.put(Communication.KEY_COMM_TYPE, HEARTBEATING);
                    try {
                        synchronized (entity.outputStream){
                            entity.outputStream.writeObject(testMessage);
                        }
                    } catch (IOException e) {
                        // socket is lost
                        socketPool.remove(key);
                        incomingHandler.connectionLost(entity.socket.getInetAddress().getHostAddress().replace("/", ""), entity.socket.getPort());
                    }
                }
            }
                try {
                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
}
