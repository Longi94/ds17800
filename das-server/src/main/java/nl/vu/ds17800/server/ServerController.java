package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.RequestStage;
import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.Communication;
import nl.vu.ds17800.core.networking.Entities.Client;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.Entities.Response;
import nl.vu.ds17800.core.networking.Entities.Server;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.PoolEntity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static nl.vu.ds17800.core.model.MessageRequest.*;
import static nl.vu.ds17800.core.model.RequestStage.ask;
import static nl.vu.ds17800.core.model.RequestStage.commit;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler {

    private Communication comm;
    private BattleField bf = new BattleField();

    private boolean initialized = false;
    // connected server peers
    private final List<Server> connectedServers;

    private Server serverDescriptor;

    // clients connected to this server
    private final Set<String> connectedClients;

    // here we reserve spots while waiting for a commit
    private final long[][] reservedSpot;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    ServerController(BattleField bf, Server serverDescr) {
        this.connectedServers = Collections.synchronizedList(new ArrayList<Server>());
        this.connectedClients = Collections.synchronizedSet(new HashSet<String>());
        reservedSpot = new long[BattleField.MAP_WIDTH][BattleField.MAP_HEIGHT];
        serverDescriptor = serverDescr;
    }

    /**
     * We do this instead of registering the handler on the Communcation instance because it
     * requires to be created with a IncomingHandler
     */
    public void setCommuncation(Communication comm) {
        this.comm = comm;
    }

    /**
     * Broadcast m to all server nodes.
     * hmm we could send to all including ourselves, that will just loopback anyway?
     * @param m message that will be sent to all servers
     * @return true if all servers accepted the message
     */
    private boolean broadcastServers(Message m) {
        m.put("requestStage", ask);

        List<Response> responses = new ArrayList<Response>();

        for (Server s : connectedServers) {
            System.out.println("Broadcasting to server " + s);
            try {
                responses.add(comm.sendMessageAsync(m, s));
            } catch (IOException e) {
                System.out.println("unhandled: FAILED TO TRANSFER MESSAGE '" + m + "' to server!");
                e.printStackTrace(System.out);
            }
        }

        Message resp = null;

        for (Response r : responses) {
            try {
                resp = r.getResponse(500);
            } catch (InterruptedException e) {
                System.out.println("Timeout! Assume that it's accepted!");
                e.printStackTrace();
            }

            if (resp != null && ((RequestStage)resp.get("requestStage"))== RequestStage.reject) {
                // got a response and it was reject! the action did not succeed
                return false;
            }
        }

        m = new Message(m);
        m.put("requestStage", commit);

        for (Server s : connectedServers) {
            try {
                comm.sendMessageAsync(m, s);
            } catch (IOException e) {
                System.out.println("unhandled: FAILED TO TRANSFER MESSAGE to server!");
            }
        }
        // do we need to check response from the last send?
        return true;
    }

    /**
     * Broadcast m to all client nodes
     * @param m message to broadcast
     */
    private void broadcastClients(Message m) {
        m = new Message(m);
        m.remove("requestStage");
        for (String c : connectedClients) {
            try {
                Client client = new Client();
                client.ipaddr = c;
                comm.sendMessageAsync(m, client);
            } catch (IOException e) {
                System.out.println("Unable to send message to client");
            }
        }
    }

    public Message handleMessage(Message m, PoolEntity connectionEntity) {
        MessageRequest request = (MessageRequest)m.get("request");
        Message reply = null;
        switch(request) {
            case clientConnect:
                // This server got a new client
                System.out.println("New client connected!");

                Unit player = null;
                if (m.get("id") != null) {
                    // this is a reconnecting client that already has a Unit
                    player = bf.findUnitById((String) m.get("id"));
                }

                if (player == null) {
                    // this is a new client that needs a Player Unit associated
                    int pos[] = bf.getRandomFreePosition();

                    // BattleField assigns the unit its position, thus -1, -1
                    if (((String)m.get("type")).equals("dragon")) {
                        player = new Dragon(this.bf.getNewUnitID(),-1, -1);
                    } else {
                        player = new Player(this.bf.getNewUnitID(),-1, -1);
                    }

                    Message msgSpawnUnit = new Message();
                    msgSpawnUnit.put("request", spawnUnit);
                    msgSpawnUnit.put("unit", player);
                    msgSpawnUnit.put("x", pos[0]);
                    msgSpawnUnit.put("y", pos[1]);

                    // Broadcast to the other servers, try a new position if we're really unlucky and some
                    // other client already took the spot
                    int retries = 0;
                    int maxRetries = 2;
                    while (!broadcastServers(msgSpawnUnit) && retries < maxRetries) {
                        pos = bf.getRandomFreePosition();
                        msgSpawnUnit.put("x", pos[0]);
                        msgSpawnUnit.put("y", pos[1]);
                        retries++;
                    }

                    if (retries == maxRetries) {
                        // 2 different positions were rejected! that is just unbelievable!
                        reply = new Message();
                        reply.put("request", clientConnect);
                        reply.put("error", "unable to spawn new unit");
                        // return to the requester
                        return reply;
                    }

                    // all servers have accepted the spawn message and should have updated their state accordingly,
                    // so we can apply it here as well!
                    bf.apply(msgSpawnUnit);

                    // well there might be some junk left on the message but..
                    broadcastClients(msgSpawnUnit);
                }

                connectedClients.add(connectionEntity.socket.getInetAddress().toString() + ":" + connectionEntity.socket.getPort());

                reply = new Message();
                reply.put("request", clientConnect);
                reply.put("id", player.getUnitID());

                // hmm we could just pass the unit map instead...
                reply.put("battlefield", bf);
                return reply;
            case clientDisconnect:
                // client gracefully disconnected! he is no longer part of the game, so we remove it
                Message msgRemoveUnit = new Message();
                Unit u = bf.findUnitById((String)m.get("id"));
                msgRemoveUnit.put("request", removeUnit);
                msgRemoveUnit.put("x", u.getX());
                msgRemoveUnit.put("y", u.getY());

                // can this be rejected?
                broadcastServers(msgRemoveUnit);
                broadcastClients(msgRemoveUnit);
                reply = new Message();
                reply.put("request", clientDisconnect);

                return reply;
            case serverConnect:
                // server node connected,
                // add to server broadcast list
                // transfer state
                reply = new Message();
                reply.put("request", serverConnect);
                reply.put(Communication.KEY_COMM_TYPE, "__response");
                reply.put(Communication.KEY_COMM_ID, m.get(Communication.KEY_COMM_ID));
                reply.put("battlefield", bf);
                try{
                    synchronized (connectionEntity.outputStream){
                        connectionEntity.outputStream.writeObject(reply);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (connectedServers){
                    Server newServ = new Server();
                    newServ.ipaddr = connectionEntity.socket.getInetAddress().toString();
                    newServ.ipaddr = newServ.ipaddr.replace("127.0.0.1","localhost").replace("/", "");
                    newServ.serverPort = (Integer)m.get("originPort");
                    boolean knownServ = false;
                    for(Server curSer : connectedServers){
                        if(newServ.equals(curSer))
                            knownServ = true;
                    }
                    if((!knownServ) && isInitialized()){
                        try {
                            connectServer(newServ);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return null;
            case serverDisconnect:
                // server node disconnected
                // what remove from broadcast list?

                // connectedServers.remove();
                return null;
            case spawnUnit:
            case moveUnit:
            case putUnit:
            case removeUnit:
            case dealDamage:
            case healDamage:

                try {
                    Message ack = new Message();
                    ack.put(Communication.KEY_COMM_ID, m.get(Communication.KEY_COMM_ID));
                    ack.put(Communication.KEY_COMM_TYPE, "__response");
                    ack.put("request", acknowledge);
                    synchronized (connectionEntity.outputStream){
                        connectionEntity.outputStream.writeObject(ack);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                RequestStage rs = (RequestStage) m.get("requestStage");
                if (rs == null && broadcastServers(m)) {
                    // accepted by servers
                    bf.apply(m);
                    broadcastClients(m);
                    return null;
                } else {
                    switch (rs) {
                        case ask:
                            if (bf.check(m)) {
                                // here we need to add the pending message to a priority list based on the the
                                // message timestamps.

                                if (request == moveUnit || request == spawnUnit) {
                                    // these are the only interactions that requires us to reserve a space
                                    long t = (long)m.get("timestamp");
                                    int x = (int)m.get("x");
                                    int y = (int)m.get("y");

                                    if (reservedSpot[x][y] != 0 && t < reservedSpot[x][y]) {
                                        // this event is more recent so we will use this!
                                        reservedSpot[x][y] = t;
                                    } else {
                                        // we have reserved this spot already! so reject!
                                        m.put("requestStage", RequestStage.reject);
                                    }
                                }

                                // this message seems valid to us!
                                m.put("requestStage", RequestStage.accept);
                            } else {
                                m.put("requestStage", RequestStage.reject);
                            }

                            return null;
                        case commit:
                            bf.apply(m);
                            broadcastClients(m);
                            if (request == moveUnit || request == spawnUnit) {
                                int x = (int)m.get("x");
                                int y = (int)m.get("y");
                                reservedSpot[x][y] = 0;
                            }
                            return null;
                        default:
                            if (broadcastServers(m)) {
                                // accepted by servers
                                bf.apply(m);
                                broadcastClients(m);
                            }
                            return null;
                    }
                }
            default:
                return Message.ack(m);
        }
    }

    @Override
    public void connectionLost(String ip, int port) {
        // some node lost connection, we don't really know if it was a client or a server

        ListIterator<Server> sit = connectedServers.listIterator();

        Server serv;

        while (sit.hasNext()) {
            serv = sit.next();

            if(serv.serverPort == port && serv.ipaddr.equals(ip)) {
                sit.remove();
                return;
            }
        }

        connectedClients.remove(ip);
    }

    public void connectServer(Server s) throws IOException, InterruptedException {
        Message m = new Message();
        m.put("request", serverConnect);
        m.put("originPort", serverDescriptor.serverPort);
        Message resp = null;
        resp = comm.sendMessage(m, s, 1000);

        // if we made it here we are sure it worked
        synchronized (connectedServers){
            connectedServers.add(s);
        }

        if (bf == null) {
            // We have not set up the BattleField instance
            bf = (BattleField) resp.get("battlefield");
        }

        System.out.println("OK! Connected!");
    }
}
