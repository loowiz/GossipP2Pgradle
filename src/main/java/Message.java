import java.io.IOException;
import java.net.*;

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

        // -------------------------------------
        byte[] sendData = "client data test".getBytes();        // Create a buffer to send
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

        clientSocket.close();   // Close the socket
    }
}

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
                serverSocket.receive(recPacket);    // Receive remote host datagram (Blocking)
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //-------------------------------------
            byte[] sendBuffer = "server data test".getBytes();
            //-------------------------------------

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer,
                    sendBuffer.length,
                    recPacket.getAddress(),
                    recPacket.getPort());

            try {
                serverSocket.send(sendPacket);      // Send the packet to the client
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (debug) System.out.println("Message sent!");
        }
    }
}
