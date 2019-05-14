import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

/**
 * Server Threads:
 * 1 thread for the game.
 * 1 thread for sending data.
 * 1 thread for each client (receiving data).
 */
class Server {

    // todo randomly generate enemies that move down
    // todo remove enemies when they go out of frame

    private static boolean acceptingConnections = true;
    private static boolean gameRunning = true;


    private StringBuilder gameState = new StringBuilder();  // the state of each object in the game is encoded in one long string.

    private static final int SEND_DELAY = 10;
    private static final int GAME_DELAY = 20;


    private int enemyGenerationRate = 50;  // number of frames before a new enemy is generated.
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


    // Start the DataSender and Game threads.
    private Server() {
        var ds = new Thread(new DataSender());
        var game = new Thread(new Game());
        ds.start();
        game.start();
        System.out.println("Server has started");
    }


    /**
     * Handler:
     * receives data from the client.
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

        // Keep receiving data from the client.
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

                player = new Player(0, 0);
                player.setName(name);
                players.add(player);

                String line;
                while ((line = in.readLine()) != null) {
                    processData(line);
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
         * Parses the string sent by the client, which contains playerActions
         * and modifies the game state according to those playerActions.
         *
         * PLAYER ACTIONS FORMAT:
         *  [Player_Name] [up] [down] [left] [right] [fire]
         *
         *  Player_Name is a String, the rest are booleans.
         *
         * @param data String sent by the Client.
         */
        private void processData(String data) {
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




    /**
     * Send the current game state to each of the clients every X milliseconds.
     *
     * GAME STATE FORMAT (full ver.)
     * - without the extra newlines between dividers
     * - the arguments in each line are separated with spaces
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
     * ENEMY_MISSILES
     * [1] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     * [2] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     * ...
     * [N] [K = number of missiles] [missile1_X] [missile1_Y] ... [missileK_X] [missileK_Y]
     *
     * STOP
     * ------------------------------------------------------------------------------------------------------------
     */
    class DataSender implements Runnable {
        @Override
        public void run() {
            while (gameRunning) {
                try {
                    Thread.sleep(SEND_DELAY);
                    updateGameStateData();
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
     * Updates the game string
     *
     * TEST FORMAT (Simpler form without missiles)
     * - without the extra newlines
     * - separated with spaces
     *
     *
     * START
     *
     * PLAYERS
     * [N = number of players]
     * [player1_Name] [player1_X] [player1_Y]
     * ...
     * [playerN_Name] [playerN_X] [playerN_Y]
     *
     * ENEMIES
     * [N = number of enemies]
     * [enemy1_X] [enemy1_Y]
     * ...
     * [enemyN_X] [enemy2_Y]
     *
     * STOP
     */
    private void updateGameStateData() {
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

        // insert player missiles HERE

        gameState.append("ENEMIES\n");
        gameState.append(enemies.size());
        gameState.append("\n");
        for (Enemy e : enemies) {
            gameState.append(e.getX());
            gameState.append(" ");
            gameState.append(e.getY());
            gameState.append("\n");
        }

        // insert enemy missiles HERE

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
         *  - collision detection
         *  - moves the objects
         *
         */
        private void timeStep() {
            for (Player p: players) {
                p.firingCounter = (p.firingCounter + 1) % (p.firingRate + 1);
                if (p.firingCounter == p.firingRate) p.fire();
            }

            removeOutOfBoundsObjects();

            enemyGenerationCounter = (enemyGenerationCounter + 1) % (enemyGenerationRate + 1);

            if (enemyGenerationCounter == enemyGenerationRate)  generateEnemy();
            for (Enemy e: enemies) { e.move(); }
            for (Player p: players) { p.move(); }

            checkCollision();
        }
    }

    /**
     * Adds a new enemy to the arrayList.
     * New Enemy:
     *      0 < x < DEFAULT_WIDTH
     *      y < 0
     */
    private void generateEnemy() {
        int x = (int) (Math.random() * 1000) % Client.DEFAULT_WIDTH;
        int y = -100;

        enemies.add(new Enemy(x, y));
    }


    /**
     * todo
     */
    private void checkCollision() {

    }


    /**
     * Removes enemies and missiles that have left the play-area
     */
    private void removeOutOfBoundsObjects() {
        int yBuffer = 100;
        enemies.removeIf(enemy -> enemy.getY() > Client.DEFAULT_HEIGHT + yBuffer);
    }



}