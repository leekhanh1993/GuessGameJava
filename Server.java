
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static Logger logger = Logger.getLogger(Server.class.getName());
    private final static int NUMPPTOSTARTGAME = 6; // number of people to start the game
    private final static int PORT = 61163;
    private final static String RECRUITMENT = "recruitment";
    private Socket connection;

    private InputStream inputStream;
    private OutputStream outputStream;

    private final boolean status = true;
    private final String phase = RECRUITMENT;

    public Server() {
        ServerSocket server = null;
        final ArrayList<PlayerHandler> connections = new ArrayList<>();

        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Server is running!!!!");
            while (status) {
                if (phase.equals(RECRUITMENT)) {
                    // print list player
                    StringBuilder sb = new StringBuilder();
                    for (PlayerHandler playerHandler : connections) {
                        sb.append(playerHandler.playerGetter().nameGetter() + "|");
                    }
                    logger.log(Level.INFO, "Players in queue: \n" + sb.toString());
                    if (connections.size() >= NUMPPTOSTARTGAME) {
                        logger.log(Level.INFO, "The game is starting.......");
                        ArrayList<PlayerHandler> playerConnections = new ArrayList<>();
                        ArrayList<PlayerHandler> tmp = new ArrayList<>();
                        for (PlayerHandler playerHandler : connections) {
                            tmp.add(playerHandler);
                        }
                        for (int i = 0; i < NUMPPTOSTARTGAME; i++) {
                            playerConnections.add(tmp.get(i));
                            connections.remove(tmp.get(i));
                        }
                        new GuessGame(playerConnections).start();

                    }
                    logger.log(Level.INFO, "Waiting for new connection....");
                    connection = server.accept();
                    outputStream = connection.getOutputStream();
                    outputStream.flush();
                    inputStream = connection.getInputStream();
                    // get player information and add them to a lobby queue.
                    final String newPlayerName = readClientRequest(inputStream);
                    final Player player = new Player(newPlayerName);
                    logger.log(Level.INFO, String.format("%s register to the game. (Address: %s, Port: %d)",
                            player.nameGetter(), connection.getInetAddress().getHostAddress(), connection.getPort()));
                    connections.add(new PlayerHandler(connection, player, connections));
                    responseToClient("waiting-Waiting the server to start the game.........");
                    readClientRequest(inputStream);
                }
            }
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

    private String readClientRequest(final InputStream inputStream) {
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

    private void responseToClient(final String s) {
        try {
            outputStream.write(s.getBytes());
            outputStream.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {
        new Server();
    }
}
