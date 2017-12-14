package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.Endpoint;
import nl.vu.ds17800.core.networking.Message;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.IncomingMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import static nl.vu.ds17800.core.model.MessageRequest.*;

public class ClientController implements IncomingHandler {

    private Unit unit;
    private String unitId;
    private BattleField battleField;
    private IUnitController unitController;
    private final PriorityBlockingQueue<Message> incomingMessages;
    private boolean requestedDisconnect = false;

    public ClientController() {
        incomingMessages = new PriorityBlockingQueue<>();
    }

    public String getUnitID() {
        return unitId;
    }

    /**
     * get next message to execute from this client
     * @return Message to be sent to server
     */
    public Message getNextMessage() {
        if(!isSpawned()) {
            return Message.nop();
        }
        return unitController.makeAction();
    }

    @Override
    public void handleMessage(IncomingMessage inm) {
        incomingMessages.add(inm.getMessage());
    }

    public void applyMessage(Message message) {
        MessageRequest request = (MessageRequest)message.get("request");

        if (battleField == null && request != clientConnect) {
            System.out.println("Ignoring message, not initialized");
            return;
        }

        switch(request) {
            case clientConnect:
                battleField = (BattleField) message.get("battlefield");
                unitId = (String)message.get("id");
                unit = battleField.findUnitById(unitId);

                // in case we reconnected, we can find our unit on the battlefield
                if (unit != null) {
                    // so this is a reconnect, recreate the controller
                    if (unit instanceof Dragon) {
                        unitController = new DragonController(battleField, unit);
                    } else {
                        unitController = new PlayerController(battleField, unit);
                    }
                }
                // otherwise we expect a spawnUnit message with the unit
                break;
            case spawnUnit:
                Unit u = (Unit)message.get("unit");
                if (u.getUnitID().equals(unitId)) {
                    unit = u;
                    if (unit instanceof Dragon) {
                        unitController = new DragonController(battleField, unit);
                    } else {
                        unitController = new PlayerController(battleField, unit);
                    }
                }
            default:
                //if message request correspond to battleField actions - pass message to battleField
                battleField.apply(message);
        }
    }

    /**
     * Ping servers and get the best one
     * @param servers
     */
    public static Endpoint getServerToConnect(List<Endpoint> servers) {

        Endpoint result = null;

        long bestPing = Integer.MAX_VALUE;

        // In the case of testing all servers are going to have negligible response time but
        // the first server in the list to be slower due to caches or similar, we therefore shuffle the list
        // to get clients somewhat distributed on the servers. In a real-world scenario the shuffling will have no
        // impact on the results
        Collections.shuffle(servers);

        System.out.println("Pinging servers...");

        Message msgPing = Message.ping();
        Message msgPong = null;
        for(Endpoint srv : servers) {
            long pingTime;
            long startPing = System.currentTimeMillis();

            Socket socket = null;
            ObjectOutputStream output = null;
            ObjectInputStream input = null;
            try {
                socket = new Socket(srv.getKey(), srv.getValue());
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
                output.writeObject(msgPing);
                msgPong = (Message)input.readObject();
            } catch (IOException e) {
                continue;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Cant close connection");
                        System.exit(-1);
                    }
                }
            }

            long endPing = System.currentTimeMillis();

            pingTime = endPing - startPing;
            if (pingTime < bestPing) {
                bestPing = pingTime;
                result = srv;
            }
        }

        return result;
    }

    public boolean isSpawned() {
        return battleField != null && unitId != null && battleField.findUnitById(unitId) != null;
    }

    public void applyIncomingMessages() {
        Message m;
        while ((m = incomingMessages.poll()) != null) {
            applyMessage(m);
        }
    }

    public void printBattleField() {
        System.out.println(battleField);
    }
}
