import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuessGame extends Thread {
    private static Logger logger = Logger.getLogger(GuessGame.class.getName());
    private ArrayList<PlayerHandler> connections;
    private Random r;

    public GuessGame(ArrayList<PlayerHandler> connections) {
        this.connections = connections;

    }

    @Override
    public void run() {
        r = new Random();
        final int secretNum = r.nextInt(13);
        logger.log(Level.INFO, String.format("The secret number is: %d", secretNum));
        ArrayList<PlayerHandler> playerround1 = new ArrayList<>();
        ArrayList<PlayerHandler> playerround2 = new ArrayList<>();
        for (int i = 0; i < (this.connections.size() / 2); i++) {
            playerround1.add(connections.get(i));
        }
        for (int i = (this.connections.size() / 2); i < connections.size(); i++) {
            playerround2.add(connections.get(i));
        }

        // start round1
        logger.log(Level.INFO, "Round 1 start");
        startEachRound(playerround1, secretNum);

        // start round2
        logger.log(Level.INFO, "Round 2 start");
        startEachRound(playerround2, secretNum);

    }

    private void startEachRound(ArrayList<PlayerHandler> eachRoundPlayers, int secretNum) {
        boolean statusRound = true;
        // start round
        for (PlayerHandler playerHandler : eachRoundPlayers) {
            playerHandler.secretNumSetter(secretNum);
            playerHandler.start();
        }

        // check round
        while (statusRound) {
            boolean checkRound = false;
            try {
                Thread.sleep(5000);
                for (PlayerHandler playerHandler : eachRoundPlayers) {
                    if (!playerHandler.isFinishGameGetter()) {
                        checkRound = true;
                    }
                }
                statusRound = checkRound;
            } catch (InterruptedException e) {
            }
        }

        // ranking all player
        String finalGuessRankingList = rankingAllPlayer(eachRoundPlayers);
        for (PlayerHandler playerHandler : eachRoundPlayers) {
            if (!playerHandler.isQuiteGameGetter()) {
                new FinalGuessRanking(playerHandler, finalGuessRankingList).start();
            }
        }
    }

    private String rankingAllPlayer(ArrayList<PlayerHandler> eachRoundPlayers) {
        // get list player which is left the guess game suddently.
        ArrayList<PlayerHandler> giveUpPlayers = new ArrayList<>();
        for (PlayerHandler playerHandler : eachRoundPlayers) {
            if (playerHandler.isQuiteGameGetter()) {
                giveUpPlayers.add(playerHandler);
            }
        }

        // arrange the player who guess secret number correctly.
        ArrayList<PlayerHandler> winnerPlayers = new ArrayList<>();
        PlayerHandler tmpw, tmpl;
        for (PlayerHandler playerHandler : eachRoundPlayers) {
            if (playerHandler.isBingoGetter() == true && !playerHandler.isQuiteGameGetter()) {
                winnerPlayers.add(playerHandler);
            }
        }
        if (!winnerPlayers.isEmpty()) {
            for (int i = 0; i < winnerPlayers.size(); i++) {
                for (int j = i + 1; j < winnerPlayers.size(); j++) {
                    if (winnerPlayers.get(i).clientAttemptCountGetter() < winnerPlayers.get(j)
                            .clientAttemptCountGetter()) {
                        tmpw = winnerPlayers.get(i);
                        winnerPlayers.set(i, winnerPlayers.get(j));
                        winnerPlayers.set(j, tmpw);
                    } else if (winnerPlayers.get(i).clientAttemptCountGetter() == winnerPlayers.get(j)
                            .clientAttemptCountGetter()) {
                        if (winnerPlayers.get(i).completeGameTimeGetter()
                                .compareTo(winnerPlayers.get(j).completeGameTimeGetter()) < 0) {
                            tmpw = winnerPlayers.get(i);
                            winnerPlayers.set(i, winnerPlayers.get(j));
                            winnerPlayers.set(j, tmpw);
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        // arrange the player who guess secret number incorrectly.
        ArrayList<PlayerHandler> loserPlayers = new ArrayList<>();
        for (PlayerHandler playerHandler : eachRoundPlayers) {
            if (playerHandler.isBingoGetter() == false && !playerHandler.isQuiteGameGetter()) {
                loserPlayers.add(playerHandler);
            }
        }
        if (!loserPlayers.isEmpty()) {
            for (int i = 0; i < loserPlayers.size(); i++) {
                for (int j = i + 1; j < loserPlayers.size(); j++) {
                    if (loserPlayers.get(i).clientAttemptCountGetter() < loserPlayers.get(j)
                            .clientAttemptCountGetter()) {
                        tmpl = loserPlayers.get(i);
                        loserPlayers.set(i, loserPlayers.get(j));
                        loserPlayers.set(j, tmpl);
                    } else if (loserPlayers.get(i).clientAttemptCountGetter() == loserPlayers.get(j)
                            .clientAttemptCountGetter()) {
                        if (loserPlayers.get(i).completeGameTimeGetter()
                                .compareTo(loserPlayers.get(j).completeGameTimeGetter()) < 0) {
                            tmpl = loserPlayers.get(i);
                            loserPlayers.set(i, loserPlayers.get(j));
                            loserPlayers.set(j, tmpl);
                        }
                    } else {
                        continue;
                    }
                }
            }
        }

        // create final guess ranking list
        ArrayList<PlayerHandler> finalGuessRankingList = new ArrayList<>();

        if (!giveUpPlayers.isEmpty()) {
            for (PlayerHandler playerHandler : giveUpPlayers) {
                finalGuessRankingList.add(playerHandler);
            }
        }

        if (!loserPlayers.isEmpty()) {
            for (PlayerHandler playerHandler : loserPlayers) {
                finalGuessRankingList.add(playerHandler);
            }
        }

        if (!winnerPlayers.isEmpty()) {
            for (PlayerHandler playerHandler : winnerPlayers) {
                finalGuessRankingList.add(playerHandler);
            }
        }
        // generate string ranking and notify all players
        StringBuilder sb = new StringBuilder();
        sb.append("Ranking: Lowest to Highest");
        sb.append("\n");
        for (int i = 0; i < finalGuessRankingList.size(); i++) {
            if (finalGuessRankingList.get(i).isQuiteGameGetter()) {
                sb.append(i + 1 + " " + finalGuessRankingList.get(i).playerGetter().nameGetter() + " (quit game)");
                sb.append("\n");
            } else {
                sb.append(i + 1 + " " + finalGuessRankingList.get(i).playerGetter().nameGetter());
                sb.append("\n");
            }

        }
        String message = "rankingr-" + sb.toString();

        logger.log(Level.INFO, sb.toString());
        return message;
    }

}