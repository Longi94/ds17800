package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

public class ConnectionTester extends Thread {
    private Map<String, PoolEntity> socketPool;
    private final String HEARTBEATING = "heartbeating";

    public ConnectionTester(Map<String, PoolEntity> pool){
        socketPool = pool;
    }

    public void run(){
        while(true){

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
                    socketPool.remove(key);
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
