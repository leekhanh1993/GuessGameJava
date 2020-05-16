import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinalGuessRanking extends Thread {
    private static Logger logger = Logger.getLogger(FinalGuessRanking.class.getName());
    private Socket connection;
    private ArrayList<PlayerHandler> connections;
    private String finalGuessRankingList;
    private PlayerHandler playerHandler;

    public FinalGuessRanking(PlayerHandler playerHandler, String finalGuessRankingList) {
        this.playerHandler = playerHandler;
        this.connection = playerHandler.connectionGetter();
        this.connections = playerHandler.connectionsGetter();
        this.finalGuessRankingList = finalGuessRankingList;

    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = connection.getOutputStream();
            outputStream.flush();
            inputStream = connection.getInputStream();
            // send ranking guess list to players who completed the game.
            responseToClient(this.finalGuessRankingList, outputStream);
            readClientRequest(inputStream);

            // ask player to play again
            String messplaymore = "continueq-Do you want to play more (p/q): ";
            responseToClient(messplaymore, outputStream);
            if (readClientRequest(inputStream).equalsIgnoreCase("p")) {
                this.connections
                        .add(new PlayerHandler(this.connection, this.playerHandler.playerGetter(), this.connections));
                responseToClient("waiting-Waiting the server to start the game.........", outputStream);
                logger.log(Level.INFO,
                        String.format("%s register to the game.", this.playerHandler.playerGetter().nameGetter()));
                readClientRequest(inputStream);
            } else {
                responseToClient("stop-Bye!!!", outputStream);
                logger.log(Level.INFO,
                        String.format("%s left the game.", this.playerHandler.playerGetter().nameGetter()));
                readClientRequest(inputStream);
                try {
                    if (outputStream != null)
                        outputStream.close();
                    if (inputStream != null)
                        inputStream.close();
                    if (this.connection != null)
                        this.connection.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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

    private void responseToClient(final String s, final OutputStream outputStream) {
        try {
            outputStream.write(s.getBytes());
            outputStream.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}