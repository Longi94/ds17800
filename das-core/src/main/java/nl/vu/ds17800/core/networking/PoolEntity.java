package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.Message;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PoolEntity {
    public Socket socket;
    public List<Message> requestBuffer = new ArrayList<Message>();
    public List<Message> responseBuffer = new ArrayList<Message>();

}
