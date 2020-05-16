import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class WarningClient extends Thread {
    private DatagramSocket ds;
    private boolean status = true;
    private Socket socket;

    public WarningClient(Socket socket) {
        try {
            this.ds = new DatagramSocket();
            this.socket = socket;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
            while (status) {
                for (int i = 10; i >= 0; i--) {
                    try {
                        Thread.sleep(1000);
                        if (i == 0) {
                            sendingDatagramPacket("Hey! Are you there ?");
                        }
                    } catch (InterruptedException e) {
                        if (!status) {
                            ds.close();
                            break;
                        }
                    }
                }
            }

    }

    public void killProc() {
        this.status = false;
        this.interrupt();
    }

    private void sendingDatagramPacket(String clientMess) {
        try {

            String str = clientMess;

            InetAddress ip = this.socket.getInetAddress();

            DatagramPacket dp = new DatagramPacket(str.getBytes(), str.length(), ip, this.socket.getPort());

            this.ds.send(dp);

        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private String receiveDatagramPacket() {
        String str = null;
        try {
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, 1024);
            ds.receive(dp);

            str = new String(dp.getData(), 0, dp.getLength());

        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return str;

    }

}