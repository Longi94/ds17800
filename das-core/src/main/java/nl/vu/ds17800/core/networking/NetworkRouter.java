package nl.vu.ds17800.core.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class NetworkRouter extends Thread{
    static private final int DSPORT = 666;
    private IncomingHandler messageHandler;
    private Map<String, PoolEntity> socketPool;
    public NetworkRouter(IncomingHandler incomingHandler, Map<String, PoolEntity> sockets){
        super();
        socketPool = sockets;
        messageHandler = incomingHandler;
    }

    public void run(){
        ServerSocket ssocket = null;
        try {
            ssocket = new ServerSocket(DSPORT);
            while(true){
                Socket socket = ssocket.accept();
                PoolEntity entity = new PoolEntity();
                entity.socket = socket;
                socketPool.put(socket.getInetAddress().toString(), entity);
                Worker worker = new Worker(messageHandler, socket);
                worker.start();
            }
        } catch (IOException e) {
        }
    }
}
