package client;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.message.Message;

import model.FileDataResponseType;
import model.FileListResponseType;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;
import model.ResponseType.RESPONSE_TYPES;

public class dummyClient {
    public static int selectedFileId;
    public static DatagramSocket datagramSocket;
    public static int packetsLost = 0;
    public static int packetSent = 0;
    public static int totalPacketsSentPort1 = 0; 
    public static int totalPacketsLostPort1 = 0;
    public static int totalPacketsSentPort2 = 0;
    public static int totalPacketsLostPort2 = 0;
    public static long totalRttPort1 = 0;
    public static long totalRttPort2 = 0;
    public static ArrayList<DatagramPacket> packetsForPort1 = new ArrayList();
    public static ArrayList<DatagramPacket> packetsForPort2 = new ArrayList();
    public static ArrayList<DatagramPacket> packetsLostForPort1 = new ArrayList();
    public static ArrayList<DatagramPacket> packetsLostForPort2 = new ArrayList();
    static {
        try {
            datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void sendInvalidRequest(String ip, int port) throws IOException{
		 InetAddress IPAddress = InetAddress.getByName(ip); 
         RequestType req=new RequestType(4, 0, 0, 0, null);
         byte[] sendData = req.toByteArray();
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
         DatagramSocket dsocket = new DatagramSocket();
         dsocket.send(sendPacket);
         byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
         DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
         dsocket.receive(receivePacket);
         ResponseType response=new ResponseType(receivePacket.getData());
         loggerManager.getInstance(this.getClass()).debug(response.toString());
	}
	
	private void getFileList(String ip, int port) throws IOException{
		InetAddress IPAddress = InetAddress.getByName(ip); 
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        datagramSocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        datagramSocket.receive(receivePacket);
        FileListResponseType response=new FileListResponseType(receivePacket.getData());
        System.out.println(response);
        loggerManager.getInstance(this.getClass()).debug(response.toString());
	}
	
	private long getFileSize(String ip, int port, int file_id) throws IOException{
		InetAddress IPAddress = InetAddress.getByName(ip); 
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, file_id, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileSizeResponseType response = new FileSizeResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());
        return response.getFileSize();
	}
	
	private void getFileData(String ip, int port, int file_id, long start, long end) throws IOException{
		InetAddress IPAddress = InetAddress.getByName(ip); 
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte=-1;
        while(maxReceivedByte<end){
        	DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
            FileDataResponseType response=new FileDataResponseType(receivePacket.getData());
            System.out.println(response);
            loggerManager.getInstance(this.getClass()).debug(response.toString());
            if (response.getResponseType() != RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
            	break;
            }
            if (response.getEnd_byte()>maxReceivedByte){
            	maxReceivedByte=response.getEnd_byte();
            };
        }
	}

    public FileDataResponseType getFileDataExclusive(String ip, int port, int file_id, long start, long end) throws IOException {
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);

        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];

        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);

        addPacketToPortTracking(port, sendPacket);

        boolean isPacketRecieved = false;

        while(!isPacketRecieved) {
            try{
                isPacketRecieved = attemptToSendAndRecievePacket(sendPacket, receivePacket, port);
            }catch(IOException e){
                handlePacketLoss(port, sendPacket);
            }
        }
        return new FileDataResponseType(receivePacket.getData());
    }

    private void addPacketToPortTracking(int port, DatagramPacket packet) {
        (port == 5000 ? packetsForPort1 : packetsForPort2).add(packet);        
    }

    private boolean attemptToSendAndRecievePacket(DatagramPacket senDatagramPacket, DatagramPacket receiveDatagramPacket, int port) throws IOException {
        datagramSocket.send(senDatagramPacket);
        datagramSocket.receive(receiveDatagramPacket);
        packetSent++;
        return true;
    }

    private void handlePacketLoss(int port, DatagramPacket packet)
    {
        System.out.println("Packet Lost!!!");
        packetsLost++;
        (port == 5000 ? packetsForPort1 : packetsForPort2).add(packet);
    }

	public static dummyClient inst = new dummyClient();
        

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        // if (args.length < 2) {
        //     throw new IllegalArgumentException("Two arguments required: ip:port1 ip:port2");
        // }
        // String[] adr1 = args[0].split(":");
        // String ip = adr1[0];
        String ip = "localhost";
        int port1 = 5001;
        int port2 = 5002;
        

        // int port1 = Integer.parseInt(args[0].split(":")[1]);
        // int port2 = Integer.parseInt(args[1].split(":")[1]);
       
        System.out.println("IP: " + ip);
        System.out.println("Port 1: " + port1);
        System.out.println("Port 2: " + port2);


        inst.getFileList(ip, port1);
        Scanner input = new Scanner(System.in);
        System.out.print("Select valid file ID: ");
        int fileId = 3;
        selectedFileId = fileId;
        long fileSize = inst.getFileSize(ip, port1, selectedFileId);
        
        long halfSize = fileSize / 2;
        System.out.println("File size: " + fileSize + " bytes");

        String tempFile1 = "C:\\Users\\emree\\OneDrive\\Masaüstü\\CS468_Project2\\KaracaBesik_proj2\\rdtp\\FileListClient\\temp1.txt";
        String tempFile2 = "C:\\Users\\emree\\OneDrive\\Masaüstü\\CS468_Project2\\KaracaBesik_proj2\\rdtp\\FileListClient\\temp2.txt";

        System.out.println(String.format("File %d has been selected. ",fileId));


        

        ExecutorService executor = Executors.newFixedThreadPool(2);
        


        long midpoint = fileSize / 2;

        downloadTask task1 = new downloadTask(ip, port1, selectedFileId, 1, midpoint, tempFile1);

        downloadTask task2 = new downloadTask(ip, port2, selectedFileId, midpoint, fileSize, tempFile2);
        
        Instant startTime = Instant.now();
        executor.submit(task1);
        executor.submit(task2);

        executor.shutdown();
     
        
        Instant endTime = Instant.now();
        
      
      
        long totalDuration= Duration.between(startTime, endTime).toMillis();

        

        totalPacketsSentPort1 = task1.getPacketsSent();
        totalPacketsLostPort1 = task1.getPacketsLost();
        totalRttPort1 = task1.getTotalRTT();
        
        long averageRttPort1 =  totalRttPort1 / Math.max(task1.getPacketsSent(), 1);

        totalPacketsSentPort2 = task2.getPacketsSent();
        totalPacketsLostPort2 = task2.getPacketsLost();
        totalRttPort2 = task2.getTotalRTT();
        long averageRttPort2 =  totalRttPort2 / Math.max(task2.getPacketsSent(), 1);

        int betterConditionPort = averageRttPort1 < averageRttPort2 ? port1 : port2;

        System.out.println("------------ Download Statistics ------------");
        System.out.println("Port 1 - Average RTT: " + averageRttPort1 + "ms, Packets Lost: " + totalPacketsLostPort1);
        System.out.println("Port 2 - Average RTT: " + averageRttPort2 + "ms, Packets Lost: " + totalPacketsLostPort2);
        System.out.println("Optimal Port: " + betterConditionPort);
        System.out.println("Total elapsed time: " + totalDuration);


        System.out.println("MD5 Checksum: " + md5sum(MessageDigest.getInstance("MD5"), new File("C:\\Users\\emree\\OneDrive\\Masaüstü\\CS468_Project2\\KaracaBesik_proj2\\rdtp\\FileListClient\\testResult.txt")));
         String mergedFilePath = "C:\\Users\\emree\\OneDrive\\Masaüstü\\CS468_Project2\\KaracaBesik_proj2\\rdtp\\FileListClient\\testResult.txt";

        mergeFiles(tempFile1, tempFile2, mergedFilePath);
        System.out.println("Files merged into " + "testResult.txt");
        
        System.out.println("Download Completed. Check 'testResult.txt' for the downloaded content.");

        //////////////////////

    }


    private static void mergeFiles(String file1Path, String file2Path, String mergedFilePath) {
        try (FileOutputStream fos = new FileOutputStream(mergedFilePath);
             FileInputStream fis1 = new FileInputStream(file1Path);
             FileInputStream fis2 = new FileInputStream(file2Path)) {

           copy(fis1, fos);
           fos.flush();
           copy(fis2, fos);
           fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copy(FileInputStream source, FileOutputStream destination) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = source.read(buffer)) > 0) {
            destination.write(buffer, 0, length);
        }
    }

   




    public static void createFile(){
        File myObj = new File("testResult.txt");
        if (!myObj.exists()) {
            try {
                if (myObj.createNewFile()) {
                    System.out.println("File created: " + myObj.getName());
                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }else {
            writeFile("");
        }
    }

    public static void writeFile(String text){
        try {
            FileWriter myWriter = new FileWriter("testResult.txt");
            myWriter.write(text);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    public static void appendFile(String text){
        try {
            Files.write(Paths.get("testResult.txt"), text.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            System.out.println("error happened!");
        }
    }
    public static void printFileSizeNIO(String fileName) {
        Path path = Paths.get(fileName);
        try {
            long bytes = Files.size(path);
            System.out.println(String.format("%,d bytes", bytes));
            System.out.println(String.format("%,d kilobytes", bytes / 1024));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static long[] calculateRTTForFileSize(int file_id, int port) throws IOException {
        Date date = new Date();
        long starting = date.getTime();
        long[] returnedValue = new long[2];
        returnedValue[0] = inst.getFileSize("127.0.0.1",port,file_id);
        Date date2 = new Date();
        long ending = date2.getTime();
        returnedValue[1] = ending - starting;
        return returnedValue;
    }

    public PacketManager calculateDataPacketRTT(int file_id, int port,long startByte,long endByte) throws IOException {

        Instant start = Instant.now();
        PacketManager PacketObject = new PacketManager();

        ResponseType responseType = inst.getFileDataExclusive("127.0.0.1",port,file_id,startByte,endByte);
        
        PacketObject.setResponseType(responseType);
        Instant end = Instant.now();
        PacketObject.setEnding(end.toEpochMilli());
        

        Duration timeElapsed = Duration.between(start, end);
        
        PacketObject.setTimeout(2*end.toEpochMilli() - start.toEpochMilli());
        PacketObject.setRTT(timeElapsed.toMillis());
        PacketObject.setPort(port);

        return PacketObject;
    
    }


    public static String md5sum(MessageDigest digest, File file) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1){ digest.update(byteArray, 0, bytesCount); }

        fis.close();

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {

            sb.append(Integer
                    .toString((bytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }

        return sb.toString();
    }
}