package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.RequestStage;
import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.*;
import nl.vu.ds17800.core.networking.Entities.Message;

import java.util.*;
import java.util.concurrent.*;

import static nl.vu.ds17800.core.model.MessageRequest.*;
import static nl.vu.ds17800.core.model.RequestStage.ask;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler, IBroadcaster {

    private final Executor executor = Executors.newCachedThreadPool();
    private final Map<UUID, FutureTask<Boolean>> broadcastFutureTasks;
    private final Map<UUID, Integer> acceptsRequired;

    /**
     * To broadcast something after a timeout, unless cancelled before
     */
    private class BroadcastOnTimeout implements Callable<Boolean> {
        private final Message message;
        public final static long TIMEOUT = 2000;
        private final IBroadcaster broadcaster;

        public BroadcastOnTimeout(Message m, IBroadcaster broadcaster) {
            this.message = m;
            this.broadcaster = broadcaster;
        }

        @Override
        public Boolean call() throws Exception {
            Thread.sleep(BroadcastOnTimeout.TIMEOUT);
            System.err.println("Warning: Did not receive all Server responses on time. Assume accepted");
            broadcaster.broadcast(message);
            return true;
        }
    }


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
        this.broadcastFutureTasks = new HashMap<UUID, FutureTask<Boolean>>();
        this.acceptsRequired = new HashMap<UUID, Integer>();
    }

    /**
     * Broadcast m to all server nodes.
     * @param m message that will be sent to all servers
     * @return true if all servers accepted the message
     */
    public void broadcastServers(Message m) {
        for (IServerConnection s: connectedServers) {
            outgoingMessages.add(new OutgoingMessage(s, m));
        }
    }

    /**
     * Broadcast m to all client nodes
     * @param m message to broadcast
     */
    public void broadcastClients(Message m) {
        for (IClientConnection c : connectedClients) {
            outgoingMessages.add(new OutgoingMessage(c, m));
        }
    }

    public void broadcast(Message m) {
        broadcastServers(m);
        broadcastClients(m);
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
                // here we go state machine!
                RequestStage rs = (RequestStage) m.get("requestStage");
                if (rs == null) {
                    // one of our client requested an action, ask our server peers if they're accept it
                    if (bf.check(m)) {
                        // we initialize a state machine of sorts to keep track of the messge if should be
                        // committed or omitted
                        Message askMsg = Message.ask(m);

                        // future task to send a message at a later stage. standard case is to cancel it
                        // all accept messages OR a single reject message is received. It will however send
                        // if all servers didn't respond within a certain time period.
                        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new BroadcastOnTimeout(Message.commit(m), this));
                        executor.execute(futureTask);

                        broadcastFutureTasks.put((UUID)askMsg.get("ref"), futureTask);
                        acceptsRequired.put((UUID)askMsg.get("ref"), connectedServers.size());

                        broadcastServers(askMsg);
                    } else {
                        System.out.println("hmmmmm client out of sync with our battlefield");
                        // the client sent a message that can't even be applied
                        // to our own battlefield! He must be totally out of sync?
                        outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
                    }
                } else {
                    // this is a server sent message, it can be that a server
                    // has accepted, rejected an ask message, asks, or requests a commit an update
                    UUID ref;
                    switch (rs) {
                        case ask:
                            handleServerAsk(inm);
                            return;
                        case commit:
                            // a new update! apply it and tell our clients
                            bf.apply(m);
                            broadcastClients(m);
                            if (request == moveUnit || request == spawnUnit) {
                                // free any reservations
                                int x = (int)m.get("x");
                                int y = (int)m.get("y");
                                reservedSpot[x][y] = 0;
                            }
                            return;
                        case accept:
                            // a server has accepted our ask message, if all servers accept we can send out a commit
                            // if a server does not reject within a certain time, we will have to assume that it's
                            // unavailable and that it accepted the message
                            ref = (UUID) m.get("ref");
                            acceptsRequired.put(ref, acceptsRequired.get(ref) - 1);
                            if (acceptsRequired.get(ref) == 0) {
                                broadcastFutureTasks.remove(ref).cancel(true);
                                broadcast(Message.commit(m));
                            }
                            return;
                        case reject:
                            ref = (UUID) m.get("ref");
                            broadcastFutureTasks.remove(ref).cancel(true);
                            // some server rejected our ask message, so we may have to tell our client but it should
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

    /**
     * Handle ask message from server other
     * @param inm incoming message
     */
    public void handleServerAsk(IncomingMessage inm) {
        Message m = inm.getMessage();
        MessageRequest request = (MessageRequest)m.get("request");
        if (!bf.check(m)) {
            // the message is can not be applied to our battlefield
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
            return;
        }

        if (request != moveUnit && request != spawnUnit) {
            // this message can be applied without further checks
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.accept(m)));
            return;
        }

        // these interactions require us to reserve a spot in the battlefield
        long t = (long) m.get("timestamp");
        int x = (int) m.get("x");
        int y = (int) m.get("y");

        if (reservedSpot[x][y] != 0 && t < reservedSpot[x][y]) {
            // this event happened earlier than the currently reserved one, so this will be accepted
            reservedSpot[x][y] = t;
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.accept(m)));
        } else {
            // this event happened later in time so the message must be rejected
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
        }
    }

    public void addClient(IClientConnection c) {
        connectedClients.add(c);
    }
    public void removeClient(IClientConnection c) {
        connectedClients.remove(c);
    }

    public void addServer(IServerConnection c) {
        connectedServers.add(c);
    }

    public void removeServer(IServerConnection c) {
        connectedServers.remove(c);
    }

    @Override
    public void handleMessage(IncomingMessage inm) {
        // handle messages on a separate thread
        incomingMessages.add(inm);
    }

}
