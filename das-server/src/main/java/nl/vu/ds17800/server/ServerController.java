package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.units.Player;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.Communication;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.response.Message;

import static nl.vu.ds17800.core.model.MessageRequest.*;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler {

    private final Communication comm;
    private final BattleField bf;

    ServerController(Communication comm, BattleField bf) {
        this.comm = comm;
        this.bf = bf;
    }

    /**
     * Broadcast m to all server nodes.
     * hmm we could send to all including ourselves, that will just loopback anyway?
     * @param m message that will be sent to all servers
     * @return true if all servers accepted the message
     */
    private boolean broadcastServers(Message m) {
        // for servers ,
        // sendMessage m
        // all accept ?
        return true;
    }

    /**
     * Broadcast m to all client nodes
     * @param m message to broadcast
     */
    private void broadcastClients(Message m) {
        // for all clients,
        // send message m
    }

    public Message handleMessage(Message m) {
        MessageRequest request = (MessageRequest)m.get("request");
        Message reply = null;
        switch(request) {
            case clientConnect:
                // This server got a new client but dont spawn a unit, client may be reconnecting for
                // we have to randomize a position for it that all servers are ok with
                System.out.println("New client connected!");
                Unit player = null;
                if (m.get("id") != null) {
                    // this is a reconnceting client that already has a Unit
                    player = bf.findUnitById((String) m.get("id"));
                }

                if (player == null) {
                    // this is a new client that needs a Player Unit associated
                    Message msgSpawnUnit = new Message();
                    msgSpawnUnit.put("request", spawnUnit);

                    int pos[] = bf.getRandomFreePosition();
                    // BattleField assigns the unit its position, thus -1, -1
                    player = new Player(this.bf,-1, -1);
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
                reply = new Message();
                reply.put("request", clientDisconnect);

                return reply;

            case serverConnect:
                // server node connected,
                // add to server broadcast list
                // transfer state
                //
                break;
            case serverDisconnect:
                // server node disconnected
                // what remove from broadcast list?
                break;

            default:
                // olol
        }
        return null;
    }
}
