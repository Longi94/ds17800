package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import nl.vu.ds17800.core.networking.response.Response;
import nl.vu.ds17800.core.networking.response.Server;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hacku on 11/21/17.
 */
public class CommunicationImpl implements Communication {
    private final String    HEARTBEATING = "heartbeating";
    private IncomingHandler incomeHandler;
    private Map<String, PoolEntity> socketPool;

    public CommunicationImpl(IncomingHandler handler, Server server){
        // For Servers
        incomeHandler = handler;
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
        NetworkRouter routerThread = new NetworkRouter(incomeHandler, socketPool, server);
        routerThread.start();
    }

    public CommunicationImpl(){
        // For Clients
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
    }

    public Response sendMessageAsync(Message message, Server dest) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        String inetAddress = dest.ipaddr;
        int DSPORT = dest.serverPort;
        PoolEntity entity;
        ObjectOutputStream oout = null;
        entity = socketPool.get(inetAddress);
        if(entity == null){
            entity = new PoolEntity();
            entity.socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
            socketPool.put(inetAddress, entity);
            oout = new ObjectOutputStream(entity.socket.getOutputStream());
            Worker worker = new Worker(incomeHandler, entity);
            worker.start();
        }
        message.put("__communicationType", "__request");
        KeyGenerator keyGen;
        keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        String keyString = secretKey.getEncoded().toString();
        message.put("__communicationKey", keyString);
        oout.writeObject(message);
        return new Response(keyString, entity.responseBuffer);
    }

    public Message sendMessage(Message message, Server dest, int timeout) throws ClassNotFoundException, NoSuchAlgorithmException, InterruptedException, IOException {
        return sendMessageAsync(message, dest).getResponse(timeout);
    };

    public List<Server> getServers(){
        return new ArrayList<Server>();
    }
//
//    public boolean registerServer() {
//        return false;
//    }
//
//    public void deregisterServer() { }
}
