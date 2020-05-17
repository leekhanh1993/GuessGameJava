import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class NotificationClient extends Thread {
    private DatagramSocket ds;

    public NotificationClient(Socket socket) {
        try {
            this.ds = new DatagramSocket(socket.getLocalPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            String serverMessage = receiveDatagramPacket();
            if (serverMessage.equals("stop")) {
                break;
            }
            System.out.println(serverMessage);
        }
    }

    private String receiveDatagramPacket() {
        String str = null;
        try {
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, 1024);
            this.ds.receive(dp);
            str = new String(dp.getData(), 0, dp.getLength());

        } catch (SocketException e) {
            return "stop";
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return str;

    }

    public void killProc() {
        this.ds.close();
    }
}