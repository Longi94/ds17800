package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.CommunicationImpl;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.Entities.Server;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.PoolEntity;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.TreeMap;

import static nl.vu.ds17800.core.model.MessageRequest.*;

public class ClientController implements IncomingHandler{

    // structure that keeps servers and amount of connected clients
    public class ServerDetailsWrapper {
        public int clientsConnected;
        public Server server;
    }

    /**
     * List of hardcoded servers
     */
    private Server myServer;

    private static final int SND_MSG_TIMEOUT = 2000;

    private CommunicationImpl communication;


    public ClientController() {
        this.communication = new CommunicationImpl(this);

    }

    /**
     * Ping servers and get the best one
     * @return server descriptor
     */
    private Server getServerToConnect(int preferredServer) {

        List<Server> servers = communication.getServers();

        if (preferredServer >= 0 && servers.size() > preferredServer) {
            return servers.get(preferredServer);
        }

        System.out.println("Pinging servers...");

        // Using TreeMap to have already ordered list of servers (by ping)
        TreeMap<Long,ServerDetailsWrapper> pingServMap = new TreeMap<Long,ServerDetailsWrapper>();

        // Amount of clients in total
        int clientsTotal = 0;
        // Threshold value of clients per server
        int clientsThreshold = 0;

        for(Server srv : servers) {
            long pingTime;
            int srvClientListSize;

            try {
                long startPing = System.currentTimeMillis();
                boolean serverReachable;

                try (Socket ignored = new Socket(srv.ipaddr, srv.serverPort)) {
                    serverReachable = true;
                } catch (IOException ignored) {
                    serverReachable = false;
                }

                long endPing = System.currentTimeMillis();
                if(serverReachable) {
                    pingTime = endPing - startPing;

                    Message message = new Message();
                    message.put("request", clientListSize);
                    Message response = communication.sendMessage(message, srv, SND_MSG_TIMEOUT);
                    srvClientListSize = (int)response.get("amount");

                    System.out.println("server: " + srv.ipaddr + " | port: " + srv.serverPort + " | ping: " + pingTime + " | connected clients: " + srvClientListSize);
                    clientsTotal += srvClientListSize;
                } else {
                    System.out.println("server: " + srv.ipaddr + " | port: " + srv.serverPort + " | not reachable");
                    continue;
                }


            } catch (Exception e) {
                System.out.println("Error while pinging server: " + srv.ipaddr + ", details: " + e.getMessage());
                continue;
            }

            ServerDetailsWrapper sdw = new ServerDetailsWrapper();
            sdw.clientsConnected = srvClientListSize;
            sdw.server = srv;

            pingServMap.put(pingTime, sdw);

        }

        clientsThreshold = (int) Math.ceil( clientsTotal / (float) pingServMap.size());

        if(!pingServMap.isEmpty()) {
            // choose server by ping (TreeMap) that has less clients connected than clientThreshold value
            for(ServerDetailsWrapper srvWrapper : pingServMap.values()) {
                if(srvWrapper.clientsConnected <= clientsThreshold) {
                    return srvWrapper.server;
                }
            }
        }
        System.out.println("Could not find any server to connect.");
        return null;
    }

    /**
     * Method that connects client to the best server available
     *
     * @param unitId - my unitId if I am a reconnecting client
     * @return - returns message received as a response
     */
    private Message connectServer(String unitId, String unitType, int preferredServer) {
        Message message = new Message();
        message.put("request", clientConnect);
        message.put("type", unitType);
        if(unitId != null) message.put("id", unitId);

        Server serverToConnect = getServerToConnect(preferredServer);

        Message response;
        try {
            System.out.println("Trying to connect server: " + serverToConnect.ipaddr + ", port: " + serverToConnect.serverPort);
            response = communication.sendMessage(message, serverToConnect, 30000);

            this.myServer = serverToConnect;
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
            return null;
        }

        return response;
    }

    /**
     * Method to perform reconnection to other server if current is down or sth
     * @return success flag
     */
    public boolean reconnectServer() {
        String unitType;
        if(DasClient.myUnit.getType() == Unit.UnitType.DRAGON) {
            unitType = "dragon";
        } else {
            unitType = "player";
        }

        Message serverResponse = connectServer(DasClient.myUnit.getUnitID(), unitType, -1);
        if (serverResponse != null) {
            System.out.println("MY UNIT ID (received from server): " + serverResponse.get("id"));
        } else {
            return false;
        }

        String myUnitId = DasClient.myUnit.getUnitID();

        //refresh battleField and myUnit to current state received by server
        DasClient.battleField = (BattleField) serverResponse.get("battlefield");
        Unit retrievedUnit = DasClient.battleField.findUnitById(myUnitId);
        if(retrievedUnit == null) {
            System.out.println("Could not find my unit on battlefield!");
            return false;
        }
        //set current state of my unit after reconnection
        DasClient.myUnit = retrievedUnit;
        return true;
    }

    /**
     * Function to initialise connection with server.
     * After successfull connection battleField and player unit are being set
     * @return succes flag
     */
    synchronized public boolean initialiseConnection(String unitType, int preferredServer) {
        Message serverResponse = connectServer(null, unitType, preferredServer);

        if(serverResponse == null) return false;
        System.out.println("MY UNIT ID (received from server): " + serverResponse.get("id"));
        String myUnitId = (String) serverResponse.get("id");

        //refresh battleField and myUnit to current state received by server
        DasClient.battleField = (BattleField) serverResponse.get("battlefield");
        DasClient.myUnit = DasClient.battleField.findUnitById(myUnitId);
        return true;
    }

    /**
     * Function sending requests to server
     * @param actionWrapper - wrapped details of action performed by player
     * @return success flag
     */
    public boolean sendUnitAction(ActionWrapper actionWrapper) {

        MessageRequest request = actionWrapper.actionType;
        Message message = new Message();
        switch(request) {

            case dealDamage:
                message.put("request", dealDamage);
                message.put("x", actionWrapper.getTargetX());
                message.put("y", actionWrapper.getTargetY());
                message.put("damage", actionWrapper.getActionPoints());
                break;

            case healDamage:
                message.put("request", healDamage);
                message.put("x", actionWrapper.getTargetX());
                message.put("y", actionWrapper.getTargetY());
                message.put("healed", actionWrapper.getActionPoints());
                break;

            case moveUnit:
                message.put("request", moveUnit);
                message.put("x", actionWrapper.getTargetX());
                message.put("y", actionWrapper.getTargetY());
                message.put("unit", actionWrapper.unit);
                break;
        }

        try {
            if (CommunicationImpl.DEBUG_LOG_ENABLED) {
                System.out.println("Client: sending message...");
            }
            communication.sendMessage(message, myServer, SND_MSG_TIMEOUT);
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
            System.out.println("Trying to reconnect...");
            return reconnectServer(); // return success flag - in that situation current action request is aborted
        }

        return true;
    }


    @Override
    synchronized public Message handleMessage(Message message, PoolEntity connectionEntity) {
        MessageRequest request = (MessageRequest)message.get("request");

        switch(request) {

            case spawnUnit:
            case putUnit:
            case healDamage:
            case dealDamage:
            case moveUnit:
            case removeUnit:
                //if message request correspond to battleField actions - pass message to battleField
                DasClient.battleField.apply(message);

                // spectator
                if (DasClient.myUnit != null && DasClient.myUnit.getHitPoints() <= 0) {
                    System.out.println(DasClient.battleField.toString());
                    System.out.println("ME | " + DasClient.myUnit);
                }
                break;

            default:
                System.out.println("Unhandled message request from server");
        }

        return Message.ack(message);
    }

    @Override
    public void connectionLost(String ipaddr, int port) {
        //just reconnect
        reconnectServer();
    }
}
