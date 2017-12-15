package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.RequestStage;
import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.*;
import nl.vu.ds17800.core.networking.Message;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static nl.vu.ds17800.core.model.MessageRequest.*;

/**
 * Handle server interaction
 */
public class ServerController implements IncomingHandler, IBroadcaster {

    private final Executor executor = Executors.newCachedThreadPool();
    private final Map<UUID, FutureTask<Void>> broadcastFutureTasks = new HashMap<UUID, FutureTask<Void>>();
    private final Map<UUID, Integer> acceptsRequired = new HashMap<UUID, Integer>();

    public void consumeIncomingMessages() {
        while (pendingIncomingMessage()) {
            handleNextMessage();
        }
    }

    /**
     * To broadcast something after a timeout, unless cancelled before
     */
    private class Timer implements Runnable {
        public final static long TIMEOUT = 2000;
        private final Runnable onTimeout;
        private final long timeout;

        public Timer(long timeout, Runnable onTimeout) {
            this.onTimeout = onTimeout;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try { Thread.sleep(Timer.TIMEOUT); } catch (InterruptedException e) {
                return;
            }
            System.err.println("Warning: Did not receive all Server responses on time. Assume accepted");
            onTimeout.run();
        }
    }

    private BattleField bf = new BattleField();
    private Random random = DasServer.RANDOM;

    // connected server peers
    private final Set<IServerConnection> connectedServers = Collections.newSetFromMap(new ConcurrentHashMap<IServerConnection, Boolean>());;

    // clients connected to this server
    private final Set<IClientConnection> connectedClients = Collections.newSetFromMap(new ConcurrentHashMap<IClientConnection, Boolean>());

    // messages to handle, sorted by timestamp
    private final PriorityBlockingQueue<IncomingMessage> incomingMessages = new PriorityBlockingQueue<IncomingMessage>();

    // messages to handle, sorted by timestamp
    private final PriorityBlockingQueue<OutgoingMessage> outgoingMessages = new PriorityBlockingQueue<OutgoingMessage>();

    // here we reserve spots while waiting for a commit
    private final long[][] reservedSpot  = new long[BattleField.MAP_WIDTH][BattleField.MAP_HEIGHT];

    public ServerController() {}

    /**
     * Broadcast m to all server nodes.
     * @param m message that will be sent to all servers
     * @return true if all servers accepted the message
     */
    public void broadcastServers(Message m) {
        for (IServerConnection s : connectedServers) {
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
        System.out.println("SEND>"+m);
        broadcastServers(m);
        broadcastClients(m);
    }

    public void flushOutgoingMessages() {
        OutgoingMessage outm;
        while((outm = outgoingMessages.poll()) != null) {
            try {
                outm.send();
            } catch (IOException e) {
                System.err.println(">>>>>>>>>Unable to send message ... message is dropped!");
            }
        }
    }

    public boolean pendingIncomingMessage() {
        return incomingMessages.peek() != null;
    }

    public void handleNextMessage() {
        IncomingMessage inm = null;
        inm = incomingMessages.poll();

        if (inm == null) {
            return;
        }

        Message m = inm.getMessage();

        MessageRequest request = (MessageRequest)m.get("request");
        Message reply = null;
        switch(request) {
            case clientConnect:
                Unit player = null;

                if (m.get("id") != null) {
                    // this is a reconnecting client that already has a Unit
                    player = bf.findUnitById((String) m.get("id"));
                }

                // should we spawn a new player?
                boolean shouldSpawn = player == null;

                if (shouldSpawn) {
                    // BattleField assigns the unit its position, thus -1, -1
                    if (((String)m.get("type")).equals("dragon")) {
                        player = new Dragon(this.bf.getNewUnitID(),-1, -1, random);
                    } else {
                        player = new Player(this.bf.getNewUnitID(),-1, -1, random);
                    }
                }

                reply = new Message();
                reply.put("id", player.getUnitID());

                // This server got a new client
                System.out.println("New client connected! " + player.getUnitID());
                ((IClientConnection)inm.getSender()).setClientId(player.getUnitID());

                // maybe bad semantics but we reuse the clientConnect type here
                reply.put("request", clientConnect);
                reply.put("battlefield", bf);
                outgoingMessages.add(new OutgoingMessage(inm.getSender(), reply));

                if (shouldSpawn) {
                    int pos[] = bf.getRandomFreePosition(random);
                    // It may happen that this request is rejected, we catch that reject message later
                    // on and try a new position in that case!
                    request(Message.spawnUnit(player, pos[0], pos[1]));
                }

                return;
            case clientDisconnect:
                // client gracefully disconnected! he is no longer part of the game, so we remove it
                Message msgRemoveUnit = new Message();
                Unit u = bf.findUnitById((String)m.get("id"));
                msgRemoveUnit.put("request", removeUnit);
                msgRemoveUnit.put("x", u.getX());
                msgRemoveUnit.put("y", u.getY());

                // can this be rejected?
                bf.apply(msgRemoveUnit);
                broadcast(msgRemoveUnit);
                return;
            case serverConnect:
                BattleField battleField = (BattleField)m.get("battlefield");

                // check the battlefield of the remote server
                if (bf.getUpdateCount() < battleField.getUpdateCount()) {
                    // this battlefield is more recent so lets use this instead!
                    // if there has been a desync from other servers for some time it replacing our own
                    // battlefield like this can result on lost units that spawned on this server
                    System.out.println("Updated own battleField");
                    bf = battleField;
                    System.out.println(bf);
                }

                return;
            case serverDisconnect:
                // server node disconnected
                // not much to do about it.
                // this is unused
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
                        System.out.println("My clients wants to do " + m);
                        request(m);
                    } else {
                        System.out.println("Client desynced! Requested to apply: " + m);
                        // the client sent a message that can't even be applied
                        // to our own battlefield! He must be totally out of sync?
                        // outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.reject(m)));
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
                                // ok everyone accepted the change!
                                broadcastFutureTasks.remove(ref).cancel(true);
                                bf.apply(m);
                                broadcast(Message.commit(m));
                            }
                            return;
                        case reject:
                            ref = (UUID) m.get("ref");
                            FutureTask t = broadcastFutureTasks.remove(ref);
                            if (t != null) {
                                t.cancel(true);
                            }
                            if ((MessageRequest)m.get("request") == spawnUnit) {
                                //System.out.println("retry to spawn");

                                int pos[] = bf.getRandomFreePosition(random);
                                // Hm some server rejected a spawn request, well the player still needs
                                // to spawn so try a new position
                                request(Message.spawnUnit((Unit)m.get("unit"), pos[0], pos[1]));
                            }
                            return;
                        default:
                            // nop
                    }
                }
            default:
                return;
        }
    }

    private void request(final Message m) {
        // we initialize a state machine of sorts to keep track of the message if should be
        // committed or omitted, the messages will have a reference so that we can keep keep track of them
        Message askMsg = Message.ask(m);

        if((MessageRequest) m.get("request") == moveUnit) {
            if (reservedSpot[(Integer) m.get("x")][(Integer) m.get("y")] != 0 &&
                    reservedSpot[(Integer) m.get("x")][(Integer) m.get("y")] < (long)m.get("timestamp")) {
                System.out.println("This spot is reserved!");
                return;
            }
        }

        // Use a FutureTask to send a message after a timeout. Standard case is
        // to cancel it before the timeout and send immediately when all servers
        // have accepted to drop it if any server rejects the request
        FutureTask futureTask = new FutureTask<Void>(new Timer(Timer.TIMEOUT, new Runnable() {
            @Override
            public void run() {
                broadcast(Message.commit(m));
            }
        }), null);
        executor.execute(futureTask);

        broadcastFutureTasks.put((UUID)askMsg.get("ref"), futureTask);
        acceptsRequired.put((UUID)askMsg.get("ref"), connectedServers.size());

        broadcastServers(askMsg);
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

        if (reservedSpot[x][y] == 0 || t < reservedSpot[x][y]) {
            // this event happened earlier than the currently reserved one, so this will be accepted
            reservedSpot[x][y] = t;
            outgoingMessages.add(new OutgoingMessage(inm.getSender(), Message.accept(m)));
        } else {
            // this event happened later in time so the message must be rejected
            System.out.println("This spot is reserved!");
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
        outgoingMessages.add(new OutgoingMessage(c, Message.serverConnect(bf)));
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
