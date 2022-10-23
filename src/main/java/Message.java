import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
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
    String sendIP;
    int sentPort;
    byte[] dataTX;
    private static final boolean DEBUG = true;

    public SendThread(String sendIP, int sentPort, byte[] dataTX) {
        this.sendIP = sendIP;
        this.sentPort = sentPort;
        this.dataTX = dataTX;
    }

    @Override
    public void run() {
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(sendIP);      // Remote host IP address
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            if (DEBUG) System.out.println("Sending data...");

            DatagramSocket clientSocket = null;
            try {
                clientSocket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            // Creating the datagram
            byte[] sendData = this.dataTX;
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sentPort);

            try {
                clientSocket.send(sendPacket);      // Send the packet to the server
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] recBuffer = new byte[1024];      // Receive buffer
            DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
            try {
                clientSocket.receive(recPacket);    // Received datagram (Blocking)
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Obtaining the info
            String info = new String(recPacket.getData(), recPacket.getOffset(), recPacket.getLength());
            System.out.println(info);

            //clientSocket.close();   // Close the socket
        }
    }
}

/**
 * This class is a thread to receive UDP data.
 */
class ReceiveThread extends Thread {
    int myPort;
    byte[] dataRX;
    Peer p1 = null;
    private static final boolean DEBUG = true;

    public ReceiveThread(int myPort, Peer p1) {
        this.myPort = myPort;
        this.p1 = p1;
    }

    public byte[] getDataRX() {
        return dataRX;
    }

    @Override
    public void run() {
        boolean activeConnection = true;

        // Creating a datagram socket with a given port:
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        while (activeConnection) {
            byte[] recBuffer = new byte[1024];      // Receive buffer
            DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length); // Create receive datagram

            try {
                serverSocket.setSoTimeout(300*1000);
                if (p1.getStatus() == 1) {
                    serverSocket.receive(recPacket);    // Receive remote host datagram (Blocking)

                    byte[] recTest = recPacket.getData();
                    int count = 0;

                    // This count the amount of valid data
                    for (int i = 0; i < recTest.length; i++) {
                        if (Byte.compare(recTest[i], (byte) '\0') == 0) {
                            break;
                        }
                        count++;
                    }

                    //byte[] sendBuffer = Arrays.copyOfRange(recPacket.getData(), 0, count);
                    this.dataRX = Arrays.copyOfRange(recPacket.getData(), 0, count);

                    //DatagramPacket sendPacket = new DatagramPacket(
                    //        sendBuffer,
                    //        sendBuffer.length,
                    //        recPacket.getAddress(),
                    //        recPacket.getPort());

                    //try {
                    //    serverSocket.send(sendPacket);      // Send the packet to the client
                    //} catch (IOException e) {
                    //    throw new RuntimeException(e);
                    //}

                    //if (DEBUG) System.out.println("Message sent!");
                }

            } catch (IOException e) {
                if (DEBUG) System.out.println("Connection timed out...");
                activeConnection = false;
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

