package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public static Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException, InterruptedException{
        return null;
    };

    /** For Clients
     *
     * @return list of master nodes, asked from middleware
     */
    public static List<Map<String, Object>> getServers(){
        return null;
    };

    /** For Servers
     * Tries to be registered as new master node
     * @return
     */
    static public boolean registerServer(){
        return false;
    };

    /** For Servers
     * Deregister Server
     * @return
     */
    static public void deregisterServer(){};
}
