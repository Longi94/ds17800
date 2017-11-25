package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException;

    /** For Clients
     *
     * @return list of master nodes, asked from middleware
     */
    public List<Map<String, Object>> getServers();

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

    public void init();
}
