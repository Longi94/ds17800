package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Client;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.Entities.Response;
import nl.vu.ds17800.core.networking.Entities.Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
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
    private static Integer mesID;

    public CommunicationImpl(IncomingHandler handler, Server server){
        // For Servers
        incomeHandler = handler;
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
        NetworkRouter routerThread = new NetworkRouter(incomeHandler, socketPool, server);
        routerThread.start();
    }

    public CommunicationImpl(IncomingHandler handler){
        // For Clients
        incomeHandler = handler;
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
    }

    private String generateMessageID(){
        synchronized (mesID){
            mesID = (mesID + 1) % Integer.MAX_VALUE;
            return String.valueOf(mesID);
        }
    }

    private static String socketKey(Server dest){
        return dest.ipaddr + ":" + dest.serverPort;
    }

    public Response sendMessageAsync(Message message, Server dest) throws IOException {
        String inetAddress = dest.ipaddr;
        int port = dest.serverPort;
        PoolEntity entity;
        ObjectOutputStream oout = null;
        String mesID = generateMessageID();
        entity = socketPool.get(CommunicationImpl.socketKey(dest));
        if(entity == null){
            entity = new PoolEntity();
            entity.socket = new Socket(InetAddress.getByName(inetAddress), port);
            socketPool.put(CommunicationImpl.socketKey(dest), entity);
            oout = new ObjectOutputStream(entity.socket.getOutputStream());
        }
        message.put("__communicationType", "__request");
        message.put("__communicationID", mesID);
        oout.writeObject(message);
        return new Response(mesID, entity.responseBuffer);
    }

    public Message sendMessage(Message message, Server dest, int timeout) throws InterruptedException, IOException {
        return sendMessageAsync(message, dest).getResponse(timeout);
    }

    public Response sendMessageAsync(Message message, Client dest) throws IOException {
        String inetAddress = dest.ipaddr;
        PoolEntity entity;
        ObjectOutputStream oout = null;
        String mesID = generateMessageID();
        entity = socketPool.get(inetAddress);
        if(entity == null){
            throw new IOException();
        }
        message.put("__communicationType", "__request");
        message.put("__communicationID", mesID);
        oout.writeObject(message);
        return new Response(mesID, entity.responseBuffer);
    }

    public Message sendMessage(Message message, Client dest, int timeout) throws InterruptedException, IOException{
        return sendMessageAsync(message, dest).getResponse(timeout);
    }

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
