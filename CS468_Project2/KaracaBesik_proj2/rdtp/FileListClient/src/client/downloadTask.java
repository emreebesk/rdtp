package client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

import model.FileDataResponseType;
import model.RequestType;
import model.ResponseType;

public class downloadTask implements Runnable {
    
    private String ip;
    private int port;
    private int fileId;
    private long startByte;
    private long endByte;
    private String outputFilePath;


    //stats
    private int packetsSent;
    private int packetsLost;
    private long totalRTT;

    public downloadTask(String ip, int port, int fileId, long startByte, long endByte, String outputFilePath) {
        this.ip = ip;
        this.port = port;
        this.fileId = fileId;
        this.startByte = startByte;
        this.endByte = endByte;
        this.packetsSent = 0;
        this.packetsLost = 0;
        this.totalRTT = 0;
        this.outputFilePath = outputFilePath;
    }


    @Override
    public void run() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath, true)) {
            InetAddress IPAddress = InetAddress.getByName(ip);
            DatagramSocket datagramSocket = new DatagramSocket();
    
            long segmentStart = this.startByte;
            long segmentEnd = this.endByte;
    
            while (segmentStart < segmentEnd) {
                long segmentSize = segmentEnd - segmentStart;
                RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, fileId, segmentStart, segmentStart + segmentSize, null);
                byte[] sendData = req.toByteArray();
                DatagramPacket sendDatagramPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
    
                boolean packetReceived = false;
                while (!packetReceived) {
                    try {
                        Instant sendTime = Instant.now();
                        datagramSocket.send(sendDatagramPacket);
                        packetsSent++;
    
                        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
                        DatagramPacket receiveDatagramPacket = new DatagramPacket(receiveData, receiveData.length);
                        datagramSocket.receive(receiveDatagramPacket);
                        Instant receiveTime = Instant.now();
    
                        long rtt = Duration.between(sendTime, receiveTime).toMillis();
                        totalRTT += rtt;
    
                        ResponseType responseType = new ResponseType(receiveDatagramPacket.getData());
                        // System.out.println("responseType.getStart_byte(): " + responseType.getStart_byte());
                        // System.out.println("segmentStart: " + segmentStart);

                        if (responseType.getStart_byte() == segmentStart && responseType.getData() != null) {
                            fileOutputStream.write(responseType.getData(), 0, responseType.getData().length); 
                            segmentStart += responseType.getData().length; 
                            packetReceived = true;
                        } else {
                            packetsLost++;
                            //System.out.println("Beklenen segment başlangıcı alınamadı veya veri null.");
                        }
                    } catch (IOException e) {
                        //System.out.println("Paket kaybı veya zaman aşımı: " + e.getMessage());
                        packetsLost++;
                        // Burada yeniden deneme mekanizması ekleyebilirsiniz.
                    }
                }
            }
            datagramSocket.close();
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public int getPacketsLost() {
        return packetsLost;
    }

    public long getTotalRTT() {
        return totalRTT;
    }

    public long getStartByte() {
        return startByte;
    }
}
