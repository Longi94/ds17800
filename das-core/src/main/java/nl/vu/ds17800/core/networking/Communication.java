package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import nl.vu.ds17800.core.networking.response.Server;

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
    public static List<Server> getServers(){
        return null;
    };

    /** For Servers
     * Tries to be registered as new master node
     * @return
     */
    public boolean registerServer();

    /** For Servers
     * Deregister Server
     * @return
     */
    public void deregisterServer();
}
