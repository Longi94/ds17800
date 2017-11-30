package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import nl.vu.ds17800.core.networking.response.Server;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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

    public Message sendMessage(Message message, Server dest) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        String inetAddress = dest.ipaddr;
        int DSPORT = dest.serverPort;
        PoolEntity entity;
        ObjectOutputStream oout = null;
        ObjectInputStream oin = null;
        entity = socketPool.get(inetAddress);
        if(entity == null){
            entity = new PoolEntity();
            entity.socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
            socketPool.put(inetAddress, entity);
            oout = new ObjectOutputStream(entity.socket.getOutputStream());
            Worker worker = new Worker(incomeHandler, entity);
            worker.start();
        } else {
            Message testMessage = new Message();
            testMessage.put("__communicationType", HEARTBEATING);
            oout = new ObjectOutputStream(entity.socket.getOutputStream());
            try{
                oout.writeObject(testMessage);
            } catch (IOException e){
                entity = new PoolEntity();
                entity.socket = new Socket(dest.ipaddr, DSPORT);
                socketPool.put(inetAddress, entity);
                Worker worker = new Worker(incomeHandler, entity);
                worker.start();
                oout = new ObjectOutputStream(entity.socket.getOutputStream());
            }
        }
        socketPool.put(inetAddress, entity);
        message.put("__communicationType", "__request");
        KeyGenerator keyGen;
        keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        String keyString = secretKey.getEncoded().toString();
        message.put("__communicationKey", keyString);
        synchronized(entity.responseBuffer){
            oout.writeObject(message);
            waiting_for_response:
            while(true){
                entity.responseBuffer.wait();
                for(Message message_i : entity.responseBuffer)
                    if(message_i.get("__communicationKey").equals(keyString)) {
                        message = message_i;
                        break waiting_for_response;
                    }
            }
        }
        entity.responseBuffer.remove(message);
        return message;
    }

    public List<Server> getServers(){
        return new ArrayList<Server>();
    }


    public boolean registerServer() {
        return false;
    }

    public void deregisterServer() {
        
    }

    public void init(IncomingHandler handler, Server server){
        // For Servers
        incomeHandler = handler;
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
        NetworkRouter routerThread = new NetworkRouter(incomeHandler, socketPool, server);
        routerThread.start();
    }

    public void init(){
        // For Clients
        socketPool = new ConcurrentHashMap<String, PoolEntity>();
    }
}
