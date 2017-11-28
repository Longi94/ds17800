package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hacku on 11/21/17.
 */
public class CommunicationImpl implements Communication {
    static private final int DSPORT = 666;
    static private final String BOOTSTRAPURL = "http://165.227.133.190:8140";
    static private final String HEARTBEATING = "heartbeating";
    static private IncomingHandler incomeHandler;
    static private HashMap<String, Socket> socketPool;

    static public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException {
        Socket socket;
        socket = socketPool.get(inetAddress);
        if(socket == null)
            socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
        Message testMessage = new Message();
        testMessage.put("type", HEARTBEATING);
        ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
        try{
            oout.writeObject(testMessage);
        } catch (IOException e){
            socket = new Socket(InetAddress.getByName(inetAddress), DSPORT);
            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());
        }
        socketPool.put(inetAddress, socket);
        oout.writeObject(message);
        return (Message)oin.readObject();
    }

    static public List<Map<String, Object>> getServers(){
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet(BOOTSTRAPURL + "/servers");
        HttpResponse response;
        try{
            response = httpclient.execute(get);
            //System.out.println(EntityUtils.toString(response.getEntity()));
            JSONArray result = new JSONArray(EntityUtils.toString(response.getEntity()));
            System.out.println(result);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public boolean registerServer() {
        return false;
    }

    static public void deregisterServer() {
        
    }

    static public void init(IncomingHandler handler){
        incomeHandler = handler;
        socketPool = new HashMap<String, Socket>();
        NetworkRouter routerThread = new NetworkRouter(incomeHandler, socketPool);
        routerThread.start();
    }
}
