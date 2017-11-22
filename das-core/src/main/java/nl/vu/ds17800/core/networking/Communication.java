package nl.vu.ds17800.core.networking;

import java.io.IOException;

/**
 * Created by hacku on 11/21/17.
 */
public interface Communication {
    public Message sendMessage(Message message, String inetAddress) throws IOException, ClassNotFoundException;
    public void init();
}
