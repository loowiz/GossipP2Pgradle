import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Message {
    private static final boolean DEBUG = true;

    /*
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        if (DEBUG) System.out.println("Welcome new peer!");
        if (DEBUG) System.out.print("Peer X IP address: ");
        String peerXIP = scanner.nextLine();
        if (DEBUG) System.out.print("Peer X Port: ");
        int peerXPort = scanner.nextInt();
        scanner.nextLine();

        if (DEBUG) System.out.print("Neighbor A IP address: ");
        String neighborAIP = scanner.nextLine();
        if (DEBUG) System.out.print("Neighbor A Port: ");
        int neighborAPort = scanner.nextInt();
        scanner.nextLine();

        if (DEBUG) System.out.print("Neighbor B IP address: ");
        String neighborBIP = scanner.nextLine();
        if (DEBUG) System.out.print("Neighbor B Port: ");
        int neighborBPort = scanner.nextInt();
        scanner.nextLine();

        if (DEBUG) System.out.println();

        ReceiveThread rcv = new ReceiveThread(peerXPort);
        rcv.start();

        SendThread snd = new SendThread(neighborAIP, neighborAPort, "hello".getBytes());
        snd.start();

        SendThread snd2 = new SendThread(neighborBIP, neighborBPort, "hello".getBytes());
        snd2.start();
    }
     */
}

/**
 * This class is a thread used to send UDP data.
 */
class SendThread extends Thread {
    private String sendIP;
    private int sendPort;
    private byte[] dataTX;
    private static final boolean DEBUG = true;

    /**
     * Constructor for the send object.
     *
     * @param sendIP the peer IP.
     * @param sendPort the peer port.
     * @param dataTX the data to send.
     */
    public SendThread(String sendIP, int sendPort, byte[] dataTX) {
        this.sendIP = sendIP;
        this.sendPort = sendPort;
        this.dataTX = dataTX;
    }

    /**
     * The concurrent method.
     */
    @Override
    public void run() {
        // Establish the IP address:
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(sendIP);      // Remote host IP address
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        if (DEBUG) System.out.println("Sending data...");

        // Creating the datagram socket:
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // Creating the datagram packet:
        byte[] sendData = this.dataTX;
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendPort);

        // Sending data:
        try {
            clientSocket.send(sendPacket);      // Send the packet to the server
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //clientSocket.close();   // Close the socket
    }
}

/**
 * This class is a thread to receive UDP data.
 */
class ReceiveThread extends Thread {
    private int myPort;
    private byte[] dataRX;
    private Peer p1 = null;
    private static final boolean DEBUG = true;
    private boolean activeConnection;

    /**
     * Constructor for the receive object.
     *
     * @param myPort is the peer port.
     * @param p1 is the peer object.
     */
    public ReceiveThread(int myPort, Peer p1, boolean activeConnection) {
        this.myPort = myPort;
        this.p1 = p1;
        this.activeConnection = activeConnection;
    }

    /**
     * Getter to dataRX.
     *
     * @return the dataRX byte.
     */
    public byte[] getDataRX() {
        return dataRX;
    }

    public void setDataRX(byte[] dataRX) {
        this.dataRX = dataRX;
    }

    public void setActiveConnection(boolean activeConnection) {
        this.activeConnection = activeConnection;
    }

    /**
     * The concurrent method.
     */
    @Override
    public void run() {
        // Creating a datagram socket with a given port:
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        if (DEBUG) System.out.println("Waiting for data...");

        // Create receive datagram:
        byte[] recBuffer = new byte[1024];
        DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);

        while (activeConnection) {
            try {
                serverSocket.setSoTimeout(10*1000);
                // Receive remote host datagram (Blocking)
                serverSocket.receive(recPacket);

                // Measure the amount of valid data:
                byte[] recTest = recPacket.getData();
                int count = 0;
                for (int i = 0; i < recTest.length; i++) {
                    if (Byte.compare(recTest[i], (byte) '\0') == 0) {
                        break;
                    }
                    count++;
                }

                // Update the received data:
                byte[] bufferRX = Arrays.copyOfRange(recPacket.getData(), 0, count);
                //setDataRX(Arrays.copyOfRange(recPacket.getData(), 0, count));
                if (DEBUG) System.out.println("New data received!");

                // Verify if it is a SEARCH
                if (bufferRX.toString().toLowerCase().contains("search")) {
                    String senderIP = p1.getRequestNeighbor().substring(7, p1.getRequestNeighbor().indexOf(":"));
                    String senderPort = p1.getRequestNeighbor().substring(
                            p1.getRequestNeighbor().indexOf(":") + 1,
                            p1.getRequestNeighbor().indexOf(" ", p1.getRequestNeighbor().indexOf(senderIP)));
                    String fileNeeded = p1.getRequestNeighbor().substring(
                            p1.getRequestNeighbor().indexOf(senderPort) + senderPort.length() + 1,
                            p1.getRequestNeighbor().length());

                    if(p1.peerFiles.contains(fileNeeded)){
                        StringBuilder builder = new StringBuilder();
                        builder.append("RESPONSE " + p1.peerIP + ":" + p1.peerPort);

                        SendThread snd = new SendThread(
                                senderIP,
                                Integer.valueOf(senderPort),
                                builder.toString().getBytes());
                        snd.run();
                        p1.setStatus(0);
                    } else {
                        if (DEBUG) System.out.println("File not found here... Another peer will take care of if!");
                        StringBuilder builder = new StringBuilder();
                        builder.append("SEARCH " + senderIP + ":" + senderPort + " " + fileNeeded);

                        p1.setRequestNeighbor(builder.toString());

                        SendThread snd = new SendThread(
                                p1.knownIP[p1.choosePeer(2)],
                                p1.knownPort[p1.choosePeer(2)],
                                builder.toString().getBytes());
                        snd.run();
                        p1.setStatus(0);
                    }

                // Verify if it is a RESPONSE
                } if (bufferRX.toString().toLowerCase().contains("response")) {
                    String ownerIP = getDataRX().toString().substring(9, getDataRX().toString().indexOf(":"));
                    String ownerPort = getDataRX().toString().substring(
                            getDataRX().toString().indexOf(":") + 1, getDataRX().toString().length());

                    System.out.println("File found: " + ownerIP + ":" + ownerPort);
                    p1.setStatus(0);
                }

                //setActiveConnection(false);

            } catch (IOException e) {
                if (DEBUG) System.out.println("Connection timed out...");
                if(p1.getStatus() == 1) {
                    System.out.println("File not found!");
                    p1.setStatus(0);
                }
                setActiveConnection(false);
                serverSocket.close();
                throw new RuntimeException(e);
            } finally {
                continue;
            }
        }
    }
}


/**
 * Class TimeOutUDP is used to cancel a communication after some time with no response.
 *
 * obs.: I made this before knowing "setSoTimeout"  =(
 */
class TimeOutUDP {
    static Timer timer;
    public int seconds;
    DatagramSocket socket = null;
    private static final boolean debug = false;

    /**
     * The constructor.
     *
     * @param seconds is the timeout.
     */
    public TimeOutUDP(int seconds, DatagramSocket socket) {
        this.seconds = seconds;
        this.socket = socket;
    }

    /**
     * This starts the timer with preset time.
     */
    public void start() {
        timer = new Timer();
        timer.schedule(new FinishTime(), this.seconds*1000);
    }

    /**
     * This stops the timer.
     */
    public void stop() {
        timer.cancel();
    }

    class FinishTime extends TimerTask {
        @Override
        public void run() {
            if(debug) System.out.println("Time's up!!");
            socket.close();
            timer.cancel();
        }
    }
}

