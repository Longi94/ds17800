package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class NetworkRouter extends Thread{
    private IncomingHandler messageHandler;
    private Map<String, PoolEntity> socketPool;
    private Server serverDescriptor;
    private ConnectionTester testerThread;

    public NetworkRouter(IncomingHandler incomingHandler, Map<String, PoolEntity> sockets, Server server){
        super();
        socketPool = sockets;
        messageHandler = incomingHandler;
        serverDescriptor = server;
        testerThread = new ConnectionTester(socketPool, incomingHandler);
        testerThread.start();
    }

    public void run() {
        int DSPORT = serverDescriptor.serverPort;
        ServerSocket ssocket = null;
        try {
            ssocket = new ServerSocket(DSPORT);
        } catch (IOException e) {
            System.err.println("Error! Port may be in use");
            System.exit(1);
        }
        System.out.println("Listening for incoming connections on: " + DSPORT);


        // Starting listning to incoming connectoins o n port

        while(true){
            Socket socket = null;
            try{
                socket = ssocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            PoolEntity entity = new PoolEntity();
            entity.socket = socket;
            try{
                entity.outputStream = new ObjectOutputStream(socket.getOutputStream());
                entity.inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("NetworkRouter: cannot take object streams for writing and reading");
                continue;
            }


            synchronized (socketPool){
                String socketKey = socket.getInetAddress() + ":"+ String.valueOf(socket.getPort());
                PoolEntity existance = socketPool.get(socketKey);
                if(existance != null){
                    try {
                        existance.socket.close();
                    } catch (Exception e) {
                        // already closed; put General Exception because of I am afraid of nullPointers :)
                    }
                }
                socketPool.put(socketKey, entity);
            }
            Worker worker = new Worker(messageHandler, entity);
            worker.start();
        }
    }
}
