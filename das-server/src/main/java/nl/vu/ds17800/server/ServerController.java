package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.RequestStage;
import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.*;
import nl.vu.ds17800.core.networking.Entities.Message;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static nl.vu.ds17800.core.model.MessageRequest.*;
import static nl.vu.ds17800.core.model.RequestStage.ask;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler {


    private BattleField bf = new BattleField();
    private Random random;

    // connected server peers
    private final Set<IServerConnection> connectedServers;

    // clients connected to this server
    private final Set<IClientConnection> connectedClients;

    // messages to handle, sorted by timestamp
    private final PriorityBlockingQueue<IncomingMessage> incomingMessages;

    // messages to handle, sorted by timestamp
    private final PriorityBlockingQueue<OutgoingMessage> outgoingMessages;

    // here we reserve spots while waiting for a commit
    private final long[][] reservedSpot;

    public ServerController() {
        this.connectedServers = Collections.synchronizedSet(new HashSet<IServerConnection>());
        this.connectedClients = Collections.synchronizedSet(new HashSet<IClientConnection>());
        this.reservedSpot = new long[BattleField.MAP_WIDTH][BattleField.MAP_HEIGHT];
        this.incomingMessages = new PriorityBlockingQueue<IncomingMessage>();
        this.outgoingMessages = new PriorityBlockingQueue<OutgoingMessage>();
        this.random = new Random(123);
    }

    /**
     * Broadcast m to all server nodes.
     * @param m message that will be sent to all servers
     * @return true if all servers accepted the message
     */
    private void broadcastServers(Message m) {
        for (IServerConnection s: connectedServers) {
            outgoingMessages.add(new OutgoingMessage(s, m));
        }
    }

    /**
     * Broadcast m to all client nodes
     * @param m message to broadcast
     */
    private void broadcastClients(Message m) {
        for (IClientConnection c : connectedClients) {
            outgoingMessages.add(new OutgoingMessage(c, m));
        }
    }

    public void flushOutgoingMessages() {
        OutgoingMessage outm;
        while((outm = outgoingMessages.poll()) != null) {
            outm.send();
        }
    }

    public void handleNextMessage() {
        IncomingMessage inm = null;
        try {
            // blocks until there's a message to take
            inm = incomingMessages.take();
        } catch (InterruptedException e) {
            System.out.println("handleNextMessage Interrupted!");
            System.exit(-1);
        }
        Message m = inm.getMessage();

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
                    // BattleField assigns the unit its position, thus -1, -1
                    if (((String)m.get("type")).equals("dragon")) {
                        player = new Dragon(this.bf.getNewUnitID(),-1, -1, random);
                    } else {
                        player = new Player(this.bf.getNewUnitID(),-1, -1, random);
                    }

                    // this is a new client that needs a Player Unit associated
                    int pos[] = bf.getRandomFreePosition(random);
                    Message msgSpawnUnit = new Message();
                    msgSpawnUnit.put("requestStage", ask);
                    msgSpawnUnit.put("request", spawnUnit);
                    msgSpawnUnit.put("unit", player);
                    msgSpawnUnit.put("x", pos[0]);
                    msgSpawnUnit.put("y", pos[1]);
                    broadcastServers(msgSpawnUnit);
                }

                reply = new Message();
                reply.put("request", clientConnect);
                reply.put("id", player.getUnitID());
                reply.put("battlefield", bf);
                outgoingMessages.add(new OutgoingMessage(inm.getSender(), reply));
                return;
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
                outgoingMessages.add(new OutgoingMessage(inm.getSender(), reply));

                return;
            case serverConnect:
                // server node connected,
                // add to server broadcast list
                // transfer state
                reply = new Message();
                reply.put("request", serverConnect);
                reply.put("battlefield", bf);
                outgoingMessages.add(new OutgoingMessage(inm.getSender(), reply));
                return;
            case serverDisconnect:
                // server node disconnected
                // what remove from broadcast list?

                // connectedServers.remove();
                return;
            case spawnUnit:
            case moveUnit:
            case putUnit:
            case removeUnit:
            case dealDamage:
            case healDamage:
                RequestStage rs = (RequestStage) m.get("requestStage");
                if (rs == null) {
                    // one of our client requested an action, ask the our server peers if they're accept it
                    if (bf.check(m)) {
                        broadcastServers(Message.ask(m));
                    } else {
                        // the client sent a message that can't even be applied
                        // to our own battlefield! He must be totally out of sync?
                        outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
                    }
                } else {
                    // this is a server sent message, it can be that a server
                    // has accepted, rejected, or requests a commit an update
                    switch (rs) {
                        case ask:
                            handleServerAsk(inm);
                            return;
                        case commit:
                            bf.apply(m);
                            broadcastClients(m);
                            if (request == moveUnit || request == spawnUnit) {
                                int x = (int)m.get("x");
                                int y = (int)m.get("y");
                                reservedSpot[x][y] = 0;
                            }
                            return;
                        case accept:
                            // what case is this???
                            // accept?
                            // check how many accepts we got or something
                            //pendingMessages.add();
                            return;
                        case reject:
                            // some server rejected our message, so we may have to tell our client but it should
                            // eventually get the conflicting message and see for himself why it was rejected
                            return;
                        default:
                            // nop
                    }
                }
            default:
                return;
        }
    }

    public void handleServerAsk(IncomingMessage inm) {
        Message m = inm.getMessage();
        MessageRequest request = (MessageRequest)m.get("request");
        if (!bf.check(m)) {
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
            return;
        }

        if (request != moveUnit && request != spawnUnit) {
            // this message seems valid to us!
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.accept(m)));
            return;
        }
        // these are the only interactions that requires us to reserve a space
        long t = (long) m.get("timestamp");
        int x = (int) m.get("x");
        int y = (int) m.get("y");

        if (reservedSpot[x][y] != 0 && t < reservedSpot[x][y]) {
            // this event is more recent so we will use this!
            reservedSpot[x][y] = t;
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.accept(m)));
        } else {
            // we have reserved this spot already! so reject!
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
        }
    }

    public void addClient(IClientConnection c) {
        connectedClients.add(c);
    }
    public void removeClient(IClientConnection c) {
        connectedClients.remove(c);
    }

    @Override
    public void handleMessage(IncomingMessage inm) {
        // handle messages on a separate thread
        incomingMessages.add(inm);
    }

}
