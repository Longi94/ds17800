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

    public Message getResponse(int timeout) throws InterruptedException {

            long curTime = System.currentTimeMillis();
            while(true){
                synchronized (responseBuffer){
                    for(Message message: responseBuffer){
                        System.out.println("in buffer : " + ((String)message.get("__communicationID")));
                        if(((String)message.get("__communicationID")).equals(messageKey)){
                            responseBuffer.remove(message);
                            return message;
                        }
                    }
                    long iterTime = System.currentTimeMillis();
                    if( Math.abs(iterTime - curTime ) > timeout)
                        throw new InterruptedException();
                }
                Thread.sleep(200);
        }

    }
}
