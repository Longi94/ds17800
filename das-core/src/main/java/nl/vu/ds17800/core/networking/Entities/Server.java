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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Server)) {
            return false;
        }
        Server s = (Server) o;
        return s.serverPort == this.serverPort &&
                s.ipaddr == this.ipaddr &&
                s.rank == this.rank;
    }
}
