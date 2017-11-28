package nl.vu.ds17800.core.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class NetworkRouter extends Thread{
    static private final int DSPORT = 666;
    private IncomingHandler messageHandler;
    private Map<String, Socket> socketPool;
    public NetworkRouter(IncomingHandler incomingHandler, Map<String, Socket> sockets){
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
                socketPool.put(socket.getInetAddress().toString(), socket);
                Worker worker = new Worker(messageHandler, socket);
                worker.start();
            }
        } catch (IOException e) {
        }
    }
}
