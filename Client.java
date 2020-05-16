
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private final static String ADDRESS = "localhost";
    private final static int PORT = 61163;

    private Socket connection;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader reader;
    private boolean status = true;

    public Client() {
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please enter your name to join the game: ");
            String playerName = reader.readLine();
            connection = new Socket(ADDRESS, PORT);
            outputStream = connection.getOutputStream();
            outputStream.flush();
            inputStream = connection.getInputStream();
            sendToServer(playerName);
            // create new thread for server notification
            NotificationClient serverNotification = new NotificationClient(connection);
            serverNotification.start();
            String clientGuessNum = "", serverMessage = "";
            while (status) {
                serverMessage = readResponseFromServer(inputStream);
                String[] tmp = serverMessage.split("-");
                String signal = tmp[0];
                String message = tmp[1];
                if (signal.equals("continueq")) {
                    System.out.println(message);
                    String userInput = reader.readLine();
                    sendToServer(userInput);
                }
                if (signal.equals("waiting") || signal.equals("ranking") 
                || signal.equals("rankingr") || signal.equals("warning")) {
                    System.out.println(message);
                    sendToServer("ok");
                }
                if (signal.equals("playing")) {
                    System.out.println(message);
                    while (true) {
                        System.out.println("You can type 'e' to quite the guess game.\nPlease enter your guess number: ");
                        clientGuessNum = reader.readLine();
                        sendToServer(clientGuessNum);
                        serverMessage = readResponseFromServer(inputStream);
                        String[] tmpplaying = serverMessage.split("-");
                        String signalplaying = tmpplaying[0];
                        String messageplaying = tmpplaying[1];
                        if (signalplaying.equals("stop")) {
                            System.out.println(messageplaying);
                            sendToServer("ok");
                            break;
                        }
                        System.out.println(messageplaying);
                    }

                }
                if (signal.equals("stop")) {
                    System.out.println(message);
                    serverNotification.killProc();
                    status = false;
                }

                // warningClient = new WarningClient();
                // warningClient.start();
                // System.out.println("Please enter your guess number: ");
                // clientGuessNum = reader.readLine();
                // sendToServer(clientGuessNum);
                // warningClient.killProc();
                // serverMessage = readResponseFromServer(inputStream);
                // String[] tmp = serverMessage.split("-");
                // String signal = tmp[0];
                // String message = tmp[1];
                // if (signal.equals("stop")) {
                // System.out.println(message);
                // break;
                // }
                // System.out.println(message);
            }

        } catch (final NumberFormatException e) {

        } catch (final UnknownHostException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null)
                    outputStream.close();
                if (inputStream != null)
                    inputStream.close();
                if (connection != null)
                    connection.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void sendToServer(final String s) {
        try {
            outputStream.write(s.getBytes());
            outputStream.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private String readResponseFromServer(final InputStream inputStream) {
        final StringBuffer stringBuffer = new StringBuffer();
        final byte[] buffer = new byte[1024];
        try {
            inputStream.read(buffer);
            for (int i = 0; i < 1024; i++) {
                if (buffer[i] == 0) {
                    break;
                }
                stringBuffer.append((char) buffer[i]);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    public static void main(final String[] args) {
        new Client();
    }

}