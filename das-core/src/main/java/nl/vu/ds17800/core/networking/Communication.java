package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.io.IOException;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException;

    /**
     *
     * @return ip address of master node, asked from middleware
     */
    public String getServer();

    /**
     * Tries to be registered as new master node
     * @return
     */
    public boolean registerServer();
    public void init();
}
