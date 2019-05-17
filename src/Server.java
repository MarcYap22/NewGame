import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

/**
 * 3 Main Threads:
 *  1 thread for the game.
 *  1 thread for sending data.
 *  1 thread for each client (receiving data).
 *
 * note: these threads may spawn more threads
 *
 */
class Server {

    // todo FEATRUES:
    // game lobby
    // player hp
    // increasing spawn rate
    // level system
    // hp bar
    // shooting chargeup
    // hit effect

    private static boolean acceptingConnections = true;
    private static boolean gameRunning = true;

    private StringBuilder gameState = new StringBuilder();  // the state of each object in the game is encoded in one long string.

    private static final int SEND_DELAY = 10;
    private static final int GAME_DELAY = 15;

    private int enemyGenerationRate = 100;  // number of frames before a new enemy is generated.
    private int enemyGenerationCounter = 0;  // the current frame

    private static CopyOnWriteArraySet<BufferedWriter> clients = new CopyOnWriteArraySet<>();
    private static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        new Server();
        var pool = Executors.newFixedThreadPool(5);
        try (var listener = new ServerSocket(6969)) {
            while (acceptingConnections) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    // Starts the DataSender and Game threads.
    private Server() {
        var ds = new Thread(new DataSender());
        var game = new Thread(new Game());
        ds.start();
        game.start();
        System.out.println("Server has started");
    }


    /**
     * Receives data from the client.
     * 1 Handler per Client.
     */
    static class Handler implements Runnable {
        Socket socket;
        String name;
        BufferedWriter out;
        BufferedReader in;
        Player player;

        Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


                /*
                Ask for the name,
                add the writer to the set of clients,
                then add a new player with the name
                 */
                this.name = in.readLine();
                System.out.println(name + " has joined the server.");
                clients.add(out);

                player = new Player(Client.PLAYER_START_X, Client.PLAYER_START_Y);
                player.setName(name);
                players.add(player);

                String line;
                while ((line = in.readLine()) != null) {
                    processData(line);
                    // todo Client sends a gameOver flag. Remove the client from the list.
                }
            } catch (IOException e) {
                System.out.println("Socket disconnected.");
            } finally {
                System.out.println(name + " has left the server.");
                clients.remove(out);
                players.remove(player);
            }
        }

        /**
         * Parses playerActions, and modifies player movement based on retrieved data.
         *
         * playerActions = a string of booleans that corresponds to player movement
         * PLAYER ACTIONS FORMAT: [movingUp] [movingDown] [movingLeft] [movingRight] [isFiring]
         *
         * @param data String sent by the Client.
         */
        private void processData(String data) {
            if (data.equals("DISCONNECT")) {
                clients.remove(player);
            }
            else {
                var args = data.split(" ");
                boolean up = Boolean.valueOf(args[0]);
                boolean down = Boolean.valueOf(args[1]);
                boolean left = Boolean.valueOf(args[2]);
                boolean right = Boolean.valueOf(args[3]);
                boolean fire = Boolean.valueOf(args[4]);

                player.isMovingDown = down;
                player.isMovingUp = up;
                player.isMovingRight = right;
                player.isMovingLeft = left;
                player.isFiring = fire;

            }
        }
    }

    /**
     * Send the current game state to each of the clients every X milliseconds.
     *
     * GAME STATE FORMAT (full ver.)
     * note:
     *  > the arguments in each line are separated with spaces
     *  > no extra newlines (\n)
     * ------------------------------------------------------------------------------------------------------------
     *
     * START
     *
     * PLAYERS
     * [N = number of players]
     * [player1_Name] [player1_X] [player1_Y]
     * ...
     * [playerN_Name] [playerN_X] [playerN_Y]
     *
     * PLAYER_MISSILES
     * [player1_Name] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     * [player2_Name] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     *  ...
     * [playerN_Name] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     *
     * ENEMIES
     * [N = number of enemies]
     * [enemy1_X] [enemy1_Y]
     * ...
     * [enemyN_X] [enemyN_Y]
     *
     * STOP
     * ------------------------------------------------------------------------------------------------------------
     *
     * todo enemy missiles? Special enemies?
     */
    class DataSender implements Runnable {
        @Override
        public void run() {
            while (gameRunning) {
                try {
                    Thread.sleep(SEND_DELAY);
                    updateGameStateString();
                    for (BufferedWriter w : clients) {
                        w.write(gameState.toString());
                        w.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Updates the gameState String
     */
    private void updateGameStateString() {
        gameState = new StringBuilder();

        gameState.append("START\n");

        gameState.append("PLAYERS\n");
        gameState.append(players.size());
        gameState.append("\n");

        for (Player p: players) {
            gameState.append(p.getName());
            gameState.append(" ");
            gameState.append(p.getX());
            gameState.append(" ");
            gameState.append(p.getY());
            gameState.append("\n");
        }

        gameState.append("PLAYER MISSILES\n");
        for (Player p: players) {
            var missiles = p.missiles;
            gameState.append(p.getName());
            gameState.append(" ");

            int mSize = missiles.size();

            gameState.append(mSize);

            if (mSize > 0) {
                gameState.append(" ");
                for (int i = 0; i < mSize - 1; i++) {
                    Missile m = missiles.get(i);
                    gameState.append(m.getX());
                    gameState.append(" ");
                    gameState.append(m.getY());
                    gameState.append(" ");
                }

                Missile m = missiles.get(mSize - 1);
                gameState.append(m.getX());
                gameState.append(" ");
                gameState.append(m.getY());
            }

            gameState.append("\n");
        }

        gameState.append("ENEMIES\n");
        gameState.append(enemies.size());
        gameState.append("\n");
        for (Enemy e : enemies) {
            gameState.append(e.getX());
            gameState.append(" ");
            gameState.append(e.getY());
            gameState.append("\n");
        }

        gameState.append("STOP\n");
    }


    /**
     * Runs the game
     */
    class Game implements Runnable {
        @Override
        public void run() {
            // GAME LOOP:
            while (gameRunning) {
                try {
                    Thread.sleep(GAME_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeStep();
            }
        }

        /**
         * Updates the game variables,
         * - collision detection
         * - moves the objects
         */
        private void timeStep() {

            for (Player p : players) {
                if (p.isFiring) {
                    p.firingCounter = (p.firingCounter + 1) % (p.firingRate + 1);
                    if (p.firingCounter == p.firingRate) p.fire();
                }
            }

            removeOutOfBoundsObjects();

            enemyGenerationCounter = (enemyGenerationCounter + 1) % (enemyGenerationRate + 1);
            if (enemyGenerationCounter == enemyGenerationRate) generateEnemy();
            for (Enemy e : enemies) {
                e.move();
            }
            for (Player p : players) {
                p.move();
                for (Missile m : p.missiles) {
                    m.move();
                }
            }

            checkCollision();
        }

        /**
         * Adds a new enemy to the arrayList.
         * New Enemy:
         * 0 < x < DEFAULT_WIDTH
         * y < 0
         */
        private void generateEnemy() {
            int x = (int) (Math.random() * 1000) % Client.DEFAULT_WIDTH;
            int y = -100;

            enemies.add(new Enemy(x, y));
        }

        private void checkCollision() {
            // Player and Enemy
            for (Enemy e: enemies) {
                for (Player p: players) {
                    if (e.getBounds().intersects(p.getBounds())) {
                        // remove enemy and decrease player hp
                        p.health--;
                        enemies.remove(e);
                    }
                }
            }

            // check if player hp is positive
            players.removeIf(p -> !p.isAlive());

            // Enemy and Missile
            for (Player p : players) {
                for (Missile m : p.missiles) {
                    for (Enemy e : enemies) {
                        if (e.getBounds().intersects(m.getBounds())) {
                            enemies.remove(e);
                            p.missiles.remove(m);
                        }
                    }
                }
            }
        }

        /**
         * Removes enemies and missiles that have left the play-area
         */
        private void removeOutOfBoundsObjects() {
            int yBuffer = 50; // todo adjust?
            enemies.removeIf(enemy -> enemy.getY() > Client.DEFAULT_HEIGHT + yBuffer);
            for (Player p : players) {
                p.missiles.removeIf(m -> m.getY() < -yBuffer);
            }
        }
    }

}