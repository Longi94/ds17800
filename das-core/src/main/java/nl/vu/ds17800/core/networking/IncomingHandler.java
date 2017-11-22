package nl.vu.ds17800.core.networking;

/**
 * interface describing regular behaviour of object which will be used
 * as a set of callbacks for handling incoming messages from other nodes
 * Created by hacku on 11/21/17.
 */
public interface IncomingHandler {
    /**
     * callback for handling message
     * @param message - Message object
     * @return
     */
    public Message handleMessage(Message message);
}
