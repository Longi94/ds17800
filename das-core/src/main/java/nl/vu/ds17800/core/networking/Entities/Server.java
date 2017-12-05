package nl.vu.ds17800.core.networking.Entities;

import java.io.Serializable;

/**
 * Created by hacku on 11/21/17.
 */
public class Server implements Serializable{
    public int rank; //  id of node described by the object
    public String ipaddr; // no comments :)
    public int serverPort;

    public Server() { }
    
    public Server(int rank, String ipaddr, int serverPort) {
        this.rank = rank;
        this.ipaddr = ipaddr;
        this.serverPort = serverPort;
    }
}
