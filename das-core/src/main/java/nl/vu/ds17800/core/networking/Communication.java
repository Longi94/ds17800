package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.Entities.Client;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.Entities.Response;
import nl.vu.ds17800.core.networking.Entities.Server;

import java.io.IOException;
import java.util.List;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public Message sendMessage(Message message, Server dest, int timeout) throws IOException, InterruptedException;
    public Response sendMessageAsync(Message message, Server dest) throws IOException;

    public Message sendMessage(Message message, Client dest, int timeout) throws IOException, InterruptedException;
    public Response sendMessageAsync(Message message, Client dest) throws IOException;
    /** For Clients
     *
     * @return list of master nodes, asked from middleware
     */
    public List<Server> getServers();

    String KEY_COMM_ID = "__communicationID";
    String KEY_COMM_TYPE = "__communicationType";

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
