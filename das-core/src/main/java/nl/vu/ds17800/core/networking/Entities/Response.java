package nl.vu.ds17800.core.networking.Entities;

import java.io.IOException;
import java.util.List;

public class Response {
    private String messageKey;
    private List<Message> responseBuffer;

    public Response(String key, List<Message> buffer){
        messageKey = key;
        responseBuffer = buffer;
    }

    public Message getResponse(int timeout) throws IOException, InterruptedException {
        synchronized (responseBuffer){
            long curTime = System.currentTimeMillis();
            while(true){
                for(Message message: responseBuffer){
                    if(message.get("__communicationID").equals(messageKey)){
                        responseBuffer.remove(message);
                        return message;
                    }
                }
                long iterTime = System.currentTimeMillis();
                if( (iterTime - curTime ) > timeout)
                    throw new IOException();
                responseBuffer.wait();
            }
        }

    }
}
