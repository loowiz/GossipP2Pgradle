import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Message {

    public static void main(String[] args) {
            ReceiveThread rcv = new ReceiveThread(9000);
            rcv.start();

            SendThread snd = new SendThread("127.0.0.1", 9000);
            snd.start();
        }

}

class SendThread extends Thread {
    String sendIP;
    int sentPort;

    public SendThread(String sendIP, int sentPort) {
        this.sendIP = sendIP;
        this.sentPort = sentPort;
    }

    @Override
    public void run() {
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(sendIP);      // Remote host IP address (server)
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            // -------------------------------------
            Scanner scanner = new Scanner(System.in);
            System.out.println("To send: ");
            byte[] sendData = scanner.nextLine().getBytes();
            //byte[] sendData = "client data test".getBytes();        // Create a buffer to send
            // -------------------------------------

            DatagramSocket clientSocket = null;
            try {
                clientSocket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            // Creating the datagram
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
 * Thread to receive UDP data.
 */
class ReceiveThread extends Thread {
    int myPort;
    private static final boolean debug = true;

    public ReceiveThread(int myPort) {
        this.myPort = myPort;
    }

    @Override
    public void run() {
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            byte[] recBuffer = new byte[1024];      // Receive buffer
            DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length); // Create receive datagram

            if (debug) System.out.println("Waiting receive...");

            try {
                serverSocket.setSoTimeout(10*1000);
                serverSocket.receive(recPacket);    // Receive remote host datagram (Blocking)

                byte[] recTest = recPacket.getData();
                int count = 0;

                for (int i = 0; i < recTest.length; i++){
                    if(Byte.compare(recTest[i], (byte) '\0') == 0){
                        break;
                    }
                    count++;
                }

                byte[] sendBuffer = Arrays.copyOfRange(recPacket.getData(), 0, count);

                DatagramPacket sendPacket = new DatagramPacket(
                        sendBuffer,
                        sendBuffer.length,
                        recPacket.getAddress(),
                        recPacket.getPort());

                try {
                    serverSocket.send(sendPacket);      // Send the packet to the client
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (debug) System.out.println("Message sent!");

            } catch (IOException e) {
                if (debug) System.out.println("Connection timed out...");
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

