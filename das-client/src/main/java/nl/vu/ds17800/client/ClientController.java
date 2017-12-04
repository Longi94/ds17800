package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.networking.CommunicationImpl;
import nl.vu.ds17800.core.networking.IncomingHandler;
import nl.vu.ds17800.core.networking.Entities.Message;
import nl.vu.ds17800.core.networking.Entities.Server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.TreeMap;

import static nl.vu.ds17800.core.model.MessageRequest.*;

public class ClientController implements IncomingHandler{

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
    private Server getServerToConnect() {
        System.out.println("Pinging servers...");

        // Using TreeMap to have already ordered list of servers (by ping)
        TreeMap<Long,Server> pingServMap = new TreeMap<Long,Server>();

        for(Server srv : communication.getServers()) {
            long pingTime;

            try {
                long startPing = System.currentTimeMillis();
                boolean serverReachable = InetAddress.getByName(srv.ipaddr).isReachable(SND_MSG_TIMEOUT);
                long endPing = System.currentTimeMillis();
                if(serverReachable) {
                    pingTime = endPing - startPing;
                    System.out.println("server: " + srv.ipaddr + " | port: " + srv.serverPort + " | ping: " + pingTime);
                } else {
                    System.out.println("server: " + srv.ipaddr + " | port: " + srv.serverPort + " | not reachable");
                    continue;
                }


            } catch (Exception e) {
                System.out.println("Error while pinging server: " + srv.ipaddr + ", details: " + e.getMessage());
                continue;
            }

            pingServMap.put(pingTime, srv);

        }

        if(!pingServMap.isEmpty()) {
            return pingServMap.get(pingServMap.firstKey());
        } else {
            System.out.println("Could not find any server to connect.");
            return null;
        }
    }

    /**
     * Method that connects client to the best server available
     *
     * @param unitId - my unitId if I am a reconnecting client
     * @return - returns message received as a response
     */
    private Message connectServer(String unitId, String unitType) {
        Message message = new Message();
        message.put("request", clientConnect);
        message.put("type", unitType);
        if(unitId != null) message.put("id", unitId);

        Server serverToConnect = getServerToConnect();

        Message response;
        try {
            System.out.println("Trying to connect server: " + serverToConnect.ipaddr + ", port: " + serverToConnect.serverPort);
            response = communication.sendMessage(message, serverToConnect, SND_MSG_TIMEOUT);
            System.out.println("Connected!");
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
        Message serverResponse = connectServer(DasClient.myUnit.getUnitID(), null);

        if(serverResponse == null) return false;

        String myUnitId = DasClient.myUnit.getUnitID();

        //refresh battleField and myUnit to current state received by server
        DasClient.battleField = (BattleField) serverResponse.get("battlefield");
        DasClient.myUnit = DasClient.battleField.findUnitById(myUnitId);
        return true;
    }

    /**
     * Function to initialise connection with server.
     * After successfull connection battleField and player unit are being set
     * @return succes flag
     */
    public boolean initialiseConnection(String unitType) {
        Message serverResponse = connectServer(null, unitType);

        if(serverResponse == null) return false;

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
            communication.sendMessage(message, myServer, SND_MSG_TIMEOUT);
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
            System.out.println("Trying to reconnect...");
            return reconnectServer(); // return success flag - in that situation current action request is aborted
        }

        return true;
    }


    @Override
    public Message handleMessage(Message message) {
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
                break;

            default:
                System.out.println("Unhandled message request from server");
        }

        return null;
    }

    @Override
    public void connectionLost(String ipaddr) {
        //just reconnect
        reconnectServer();
    }
}