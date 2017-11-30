package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class NetworkRouter extends Thread{
    private IncomingHandler messageHandler;
    private Map<String, PoolEntity> socketPool;
    private Server serverDescriptor;

    public NetworkRouter(IncomingHandler incomingHandler, Map<String, PoolEntity> sockets, Server server){
        super();
        socketPool = sockets;
        messageHandler = incomingHandler;
        serverDescriptor = server;
    }

    public void run(){
        int DSPORT = serverDescriptor.serverPort;
        ServerSocket ssocket = null;
        try {
            ssocket = new ServerSocket(DSPORT);
        } catch (IOException e) {
        }
        while(true){
            Socket socket = null;
            try{
                socket = ssocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            PoolEntity entity = new PoolEntity();
            entity.socket = socket;
            socketPool.put(socket.getInetAddress().toString(), entity);
            Worker worker = new Worker(messageHandler, entity);
            worker.start();
        }
    }
}
