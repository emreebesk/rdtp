package client;

import model.ResponseType;

public class PacketManager {
    long RTT;
    int port;
    byte[] packetData;
    long timeout;
    long ending = 0;
    ResponseType responseType;

    public long getRTT() {
        return RTT;
    }

    public void setRTT(long RTT) {
        this.RTT = RTT;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    
    public byte[] getPacketData() {
        return packetData;
    }

    public void setPacketData(byte[] packetData) 
    {
        this.packetData = packetData;
    }
    
    public long getEnding() {
        return ending;
    }
    
    
    public void setEnding(long ending) 
    {
        this.ending = ending;
    }
    
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }


    public ResponseType getResponseType() {
        return responseType;
    }
    
    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
        this.packetData = responseType.getData();
    }
}
