package nl.vu.ds17800.server;

import nl.vu.ds17800.core.networking.Entities.Message;

public interface IBroadcaster {
    public void broadcastClients(Message m);
    public void broadcastServers(Message m);
    public void broadcast(Message m);
}
