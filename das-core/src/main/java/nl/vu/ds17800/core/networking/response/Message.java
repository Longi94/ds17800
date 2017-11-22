package nl.vu.ds17800.core.networking.response;

import java.io.Serializable;

/**
 * Created by hacku on 11/21/17.
 */
public class Message implements Serializable {
    public String type;
    public SerMessage data;
    public Server server;
}
