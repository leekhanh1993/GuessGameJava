import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerHandler extends Thread {
    private static Logger logger = Logger.getLogger(PlayerHandler.class.getName());
    private final String PLAYING = "playing";
    private final String RANKING = "ranking";
    private final String CONTINUEQ = "continueq";
    private final int ATTEMPT = 4;
    private final ArrayList<PlayerHandler> connections;
    private final Socket connection;
    private final Player player;
    private int secretNum;
    private String phase = PLAYING;
    private boolean status = true;
    private LocalDateTime completeGameTime;
    private boolean isFinishGame = false;
    private boolean isBingo;
    private boolean isQuiteGame;
    private int clientAttemptCount = 0;

    public PlayerHandler(final Socket connection, final Player player, final ArrayList<PlayerHandler> connections) {
        this.connection = connection;
        this.player = player;
        this.connections = connections;
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = connection.getOutputStream();
            outputStream.flush();
            inputStream = connection.getInputStream();
            this.isQuiteGame = false;
            final String message = "playing-" + this.player.nameGetter() + ".The Game start";
            responseToClient(message, outputStream);
            while (status) {
                if (phase.equals(PLAYING)) {
                    int counter = 1;
                    do {
                        // set timeout to remind player to enter secret number
                        connection.setSoTimeout(10000);
                        String clientResponse = readClientRequest(inputStream);
                        // reset inputstream after handle SocketTimeOutException when players do not
                        // send any response to the server.
                        if (clientResponse.isEmpty()) {
                            connection.setSoTimeout(0);
                            counter--;
                            continue;
                        }
                        // reset timeout if user send response to the server.
                        connection.setSoTimeout(0);
                        String result, conditionCode;
                        String tmp[];
                        int clientGuessNum;
                        if (clientResponse.equals("e")) {
                            this.isQuiteGame = true;
                            this.isFinishGame = true;
                            this.phase = CONTINUEQ;
                            String quiteMessage = "stop-You has left the guess game!!!";
                            responseToClient(quiteMessage, outputStream);
                            logger.log(Level.INFO,
                                    String.format("%s give up the guess game!!!!", this.player.nameGetter()));
                            readClientRequest(inputStream);
                            break;
                        } else {
                            if (checkClientInput(clientResponse)) {
                                this.clientAttemptCount = counter;
                                clientGuessNum = Integer.parseInt(clientResponse);
                                result = checkUserGuessNum(clientGuessNum, secretNum, counter);
                                tmp = result.split("-");
                                conditionCode = tmp[0];
                            } else {
                                counter--;
                                responseToClient("warning-Please enter a number which is in range between 0 and 12!!!",
                                        outputStream);
                                continue;
                            }
                        }
                        if (conditionCode.equals("stopw")) {
                            responseToClient("stop-" + tmp[1], outputStream);
                            logger.log(Level.INFO, String.format("%s guesss %d at attemp %d", this.player.nameGetter(),
                                    clientGuessNum, counter));
                            logger.log(Level.INFO, String.format("%s guesses the right secret number with %d attempts.",
                                    this.player.nameGetter(), counter));
                            readClientRequest(inputStream);
                            this.completeGameTime = LocalDateTime.now();
                            this.isBingo = true;
                            this.phase = RANKING;
                            this.isFinishGame =true;
                            break;
                        }
                        if (conditionCode.equals("stopl")) {
                            logger.log(Level.INFO, String.format("%s guesss %d at attemp %d", this.player.nameGetter(),
                                    clientGuessNum, counter));
                            logger.log(Level.INFO, String.format("%s guesses the wrong secret number with %d attempts.",
                                    this.player.nameGetter(), counter));
                            responseToClient("stop-" + tmp[1], outputStream);
                            readClientRequest(inputStream);
                            this.completeGameTime = LocalDateTime.now();
                            this.isBingo = false;
                            this.phase = RANKING;
                            this.isFinishGame =true;
                            this.clientAttemptCount += 1;
                            break;
                        }
                        logger.log(Level.INFO, String.format("%s guesss %d at attemp %d", this.player.nameGetter(),
                                clientGuessNum, counter));
                        responseToClient(result, outputStream);
                    } while (counter++ < ATTEMPT);
                }
                if (phase.equals(RANKING)) {
                    final String messplaymore = "ranking-Please wait for the final guess ranking....";
                    responseToClient(messplaymore, outputStream);
                    readClientRequest(inputStream);
                    this.status = false;
                }
                if (phase.equals(CONTINUEQ)) {
                    final String messplaymore = "continueq-Do you want to play more (p/q): ";
                    responseToClient(messplaymore, outputStream);
                    if (readClientRequest(inputStream).equalsIgnoreCase("p")) {
                        this.connections.add(new PlayerHandler(this.connection, this.player, this.connections));
                        responseToClient("waiting-Waiting the server to start the game.........", outputStream);
                        logger.log(Level.INFO, String.format("%s register to the game.", this.player.nameGetter()));
                        readClientRequest(inputStream);
                        this.status = false;
                    } else {
                        responseToClient("stop-Bye!!!", outputStream);
                        logger.log(Level.INFO, String.format("%s left the game.", this.player.nameGetter()));
                        readClientRequest(inputStream);
                        this.status = false;
                        try {
                            if (outputStream != null)
                                outputStream.close();
                            if (inputStream != null)
                                inputStream.close();
                            if (this.connection != null)
                                this.connection.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkClientInput(String clientResponse) {
        if (clientResponse.isEmpty()) {
            return false;
        }
        try {
            int clientGuessNum = Integer.parseInt(clientResponse);
            if (clientGuessNum >= 0 && clientGuessNum <= 12) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    private String checkUserGuessNum(final int clientGuessNum, final int secretNum, final int counter) {
        final StringBuilder sb = new StringBuilder();

        // check user's guess number
        if (counter == ATTEMPT && clientGuessNum == secretNum) {
            sb.append("stopw-");
            sb.append("Congratulation!");
        } else if (counter == ATTEMPT && clientGuessNum > secretNum) {
            sb.append("stopl-");
            sb.append("The client’s guess number is bigger than the generated number \n");
            sb.append("The secret number is : " + secretNum);
        } else if (counter == ATTEMPT && clientGuessNum < secretNum) {
            sb.append("stopl-");
            sb.append("The client’s guess number is smaller than the generated number \n");
            sb.append("The secret number is : " + secretNum);
        } else if (clientGuessNum > secretNum) {
            sb.append(String.valueOf(counter) + "-");
            sb.append("The client’s guess number is bigger than the generated number \n");
        } else if (clientGuessNum < secretNum) {
            sb.append(String.valueOf(counter) + "-");
            sb.append("The client’s guess number is smaller than the generated number \n");
        } else {
            sb.append("stopw-");
            sb.append("Congratulation!");
        }
        return sb.toString();
    }

    public boolean isFinishGameGetter() {
        return this.isFinishGame;
    }

    public boolean isQuiteGameGetter() {
        return this.isQuiteGame;
    }

    public int clientAttemptCountGetter() {
        return this.clientAttemptCount;
    }

    public String phaseGetter() {
        return this.phase;
    }

    public boolean isBingoGetter() {
        return this.isBingo;
    }

    public void isBingoSetter(final boolean isBingo) {
        this.isBingo = isBingo;
    }

    public LocalDateTime completeGameTimeGetter() {
        return this.completeGameTime;
    }

    public void completeGameTimeSetter(final LocalDateTime complDateTime) {
        this.completeGameTime = complDateTime;
    }

    public int NumGetter() {
        return this.secretNum;
    }

    public void secretNumSetter(final int secretNum) {
        this.secretNum = secretNum;
    }

    public Player playerGetter() {
        return this.player;
    }

    public Socket connectionGetter() {
        return this.connection;
    }

    public ArrayList<PlayerHandler> connectionsGetter() {
        return this.connections;
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
            // remind clients to response.
            DatagramSocket ds;
            try {
                ds = new DatagramSocket();
                String str = "Are you there!!!!";

                InetAddress ip = this.connection.getInetAddress();

                DatagramPacket dp = new DatagramPacket(str.getBytes(), str.length(), ip, this.connection.getPort());

                ds.send(dp);

            } catch (SocketException e1) {
                System.out.println(e1.getMessage());
            } catch (UnknownHostException e1) {
                System.out.println(e1.getMessage());
            } catch (IOException e1) {
                System.out.println(e1.getMessage());
            }
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