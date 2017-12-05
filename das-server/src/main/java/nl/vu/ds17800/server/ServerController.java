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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static nl.vu.ds17800.core.model.MessageRequest.*;
import static nl.vu.ds17800.core.model.RequestStage.ask;
import static nl.vu.ds17800.core.model.RequestStage.commit;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler {

    private Communication comm;
    private BattleField bf;

    // connected server peers
    private final ArrayList<Server> connectedServers;

    // clients connected to this server
    private final ArrayList<Client> connectedClients;

    ServerController(BattleField bf) {
        this.connectedServers = new ArrayList<Server>();
        this.connectedClients = new ArrayList<Client>();
    }

    /**
     * We do this intead of registering the handler on the Communcation instance because it
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
            try {
                responses.add(comm.sendMessageAsync(m, s));
            } catch (IOException e) {
                System.out.println("unhandled: FAILED TO TRANSFER MESSAGE to server!");
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
        for (Client c : connectedClients) {
            try {
                comm.sendMessageAsync(m, c);
            } catch (IOException e) {
                System.out.println("Unable to send message to client");
            }
        }
    }

    public Message handleMessage(Message m) {
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
                reply.put("battlefield", bf);
                return reply;
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
                RequestStage rs = (RequestStage) m.get("requestStage");
                switch (rs) {
                    case ask:
                        if (bf.check(m)) {
                            // here we need to add the pending message to a priority list based on the the
                            // message timestamps.

                            // this message seems valid to us!
                            m.put("requestStage", RequestStage.accept);
                        } else {
                            m.put("requestStage", RequestStage.reject);
                        }

                        return m;
                    case commit:
                        bf.apply(m);
                        broadcastClients(m);
                    default:
                        if (broadcastServers(m)) {
                            // accepted by servers
                            bf.apply(m);
                            broadcastClients(m);
                        }
                        return null;
                }
            default:
                return null;
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

        ListIterator<Client> cit = connectedClients.listIterator();

        Client client;
        while (cit.hasNext()) {
            client = cit.next();
            if (client.ipaddr.equals(ip)) {
                cit.remove();
                return;
            }
        }

    }

    public void connectServer(Server s) throws IOException, InterruptedException {
        Message m = new Message();
        m.put("request", serverConnect);
        Message resp = null;
        resp = comm.sendMessage(m, s, 1000);

        // if we made it here we are sure it worked
        connectedServers.add(s);

        if (bf == null) {
            // We have not set up the BattleField instance
            bf = (BattleField) resp.get("battlefield");
        }

        System.out.println("ok got response.." + resp.get("request"));
    }
}
