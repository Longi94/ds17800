package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;
import nl.vu.ds17800.core.networking.response.Response;
import nl.vu.ds17800.core.networking.response.Server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public Message sendMessage(Message message, Server dest, int timeout) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException;
    public Response sendMessageAsync(Message message, Server dest) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException;
    /** For Clients
     *
     * @return list of master nodes, asked from middleware
     */
    public List<Server> getServers();

    /** For Servers
     * Tries to be registered as new master node
     * @return
     */
//    public boolean registerServer();
//
//    /** For Servers
//     * Deregister Server
//     * @return
//     */
//    public void deregisterServer();
}
