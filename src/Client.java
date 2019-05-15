import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The client has 3 threads:
 *  1 - sends data
 *  2 - receives data
 *  3 - displays the game
 */
public class Client {
    private Socket socket;
    private BufferedWriter out;
    private BufferedReader in;
    private static final int SEND_DELAY = 20;
    private static final int RECEIVE_DELAY = 10;

    private String name;
    private Player player;

    private CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();

    /**
     * Connects to the server,
     * instantiates the IO streams,
     * then starts the sender and receiver threads.
     */
    private Client() {
        try {
            socket = new Socket(HOST_NAME, PORT_NUM);
            System.out.println("successfully connected to : " + socket.getRemoteSocketAddress());
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        GameFrame g = new GameFrame();
        g.setUpFrame();
    }

    /**
     * Sends the playerAction String to the Server.
     * The playerAction String contains instructions for player movement.
     *
     * PLAYER ACTIONS FORMAT:   [movingUp] [movingDown] [movingLeft] [movingRight] [isFiring]
     *
     *  Player_Name <- string
     *  rest <- boolean
     *
     */
    class DataSender implements Runnable {
        StringBuilder playerActions = new StringBuilder();

        /* Builds the player actions string */
        void getPlayerActions() {
            playerActions.append(player.isMovingUp);
            playerActions.append(" ");
            playerActions.append(player.isMovingDown);
            playerActions.append(" ");
            playerActions.append(player.isMovingLeft);
            playerActions.append(" ");
            playerActions.append(player.isMovingRight);
            playerActions.append(" ");
            playerActions.append(player.isFiring);
            playerActions.append("\n");

        }

        @Override
        public void run() {
            try {
                out.write(name);
                out.newLine();
                out.flush();
                System.out.println("name sent");
                while (true) {
                    Thread.sleep(SEND_DELAY);
                    getPlayerActions();
                    out.write(playerActions.toString());
                    out.flush();
                    playerActions.delete(0, playerActions.length());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Periodically receives data from the Server and processes it
     * todo thread scheduling?
     */
    class DataReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                StringBuilder data = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    data.append(line);
                    data.append("\n");
                    if (line.equals("STOP")) {
                        processData(data.toString());
                        data.delete(0, data.length());
                        Thread.sleep(RECEIVE_DELAY);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
     * STOP
     * ------------------------------------------------------------------------------------------------------------
     */
    private void processData(String data) {
        Scanner in = new Scanner(data);
        enemies.clear();
        players.clear();
        int numPlayers = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.equals("PLAYERS")) {
                numPlayers = Integer.parseInt(in.nextLine());
                for (int i = 0; i < numPlayers; i++) {
                    var playerInfo = in.nextLine().split(" ");
                    String playerName = playerInfo[0];
                    int playerX = Integer.parseInt(playerInfo[1]);
                    int playerY = Integer.parseInt(playerInfo[2]);

                    // Look for this player in the array list.
                    // if it's there, then update the x and y
                    // if it's not there, then add it

                    Player player = new Player(playerX, playerY);
                    player.setName(playerName);
                    players.add(player);
                }
            } else if(line.equals("PLAYER MISSILES")) {
                for (int i = 0; i < numPlayers; i++) {
                    var missilesLine = in.nextLine().split(" ");
                    String playerName = missilesLine[0];
                    int numMissiles = Integer.parseInt(missilesLine[1]);


                    // add the missiles to the player
                    if (numMissiles > 0) {
                        for (Player p : players) {
                            if (p.getName().equals(playerName)) {
                                for (int j = 2; j < 2 + numMissiles * 2; j = j + 2) {
                                    int missileX = Integer.parseInt(missilesLine[j]);
                                    int missileY = Integer.parseInt(missilesLine[j + 1]);
                                    p.addMissile(new Missile(missileX, missileY));
                                }
                                break;
                            }
                        }
                    }
                }

            } else if (line.equals("ENEMIES")){
                int numEnemies = Integer.parseInt(in.nextLine());
                for (int i = 0; i < numEnemies; i++) {
                    var enemyInfo = in.nextLine().split(" ");
                    int enemyX = Integer.parseInt(enemyInfo[0]);
                    int enemyY = Integer.parseInt(enemyInfo[1]);

                    // Update the enemy in the ArrayList
                    // If the enemy doesn't exist, add a new one
                        Enemy e = new Enemy(enemyX, enemyY);
                        enemies.add(e);
                }
            } // if "END", just ignore
        }
    }
    
    class GameFrame extends JFrame {
        GamePanel gamePanel = new GamePanel();
        
        void setUpFrame() {
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            this.getContentPane().add(gamePanel);
        }

        private class GamePanel extends JPanel implements Runnable {
            private final int GAME_DELAY = 20;

            GamePanel() {
                setBackground(Color.black);
                setFocusable(true);
                addListeners();

                var animator = new Thread(this);
                animator.start();
            }


            private void addListeners() {
                addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        int k = e.getKeyCode();

                        switch (k) {
                            case KeyEvent.VK_W:
                                player.isMovingUp = true;
                                break;
                            case KeyEvent.VK_A:
                                player.isMovingLeft = true;
                                break;
                            case KeyEvent.VK_S:
                                player.isMovingDown = true;
                                break;
                            case KeyEvent.VK_D:
                                player.isMovingRight = true;
                                break;
                            case KeyEvent.VK_SPACE:
                                player.isFiring = true;
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        int k = e.getKeyCode();

                        switch (k) {
                            case KeyEvent.VK_W:
                                player.isMovingUp = false;
                                break;
                            case KeyEvent.VK_A:
                                player.isMovingLeft = false;
                                break;
                            case KeyEvent.VK_S:
                                player.isMovingDown = false;
                                break;
                            case KeyEvent.VK_D:
                                player.isMovingRight = false;
                                break;
                            case KeyEvent.VK_SPACE:
                                player.isFiring = false;
                        }
                    }
                });
            }

            @Override
            public void run() {
                // ask for name and send it
                // then add the player to the list
                name = JOptionPane.showInputDialog("Enter your name:");
                player = new Player(PLAYER_START_X, PLAYER_START_Y);
                player.setName(name);
                players.add(player);

                var sender = new Thread(new DataSender());  // send data
                var receiving = new Thread(new DataReceiver());  // receive data
                sender.start();
                receiving.start();

                // this loop:
                // moves objects (to hide latency)
                // repaints
                while (true) {
                    for (Player p: players) {
                        p.move();
                    }
                    for (Enemy e: enemies) {
                        e.move();
                    }
                    repaint();
                    try {
                        Thread.sleep(GAME_DELAY);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }


            @Override
            protected void paintComponent(Graphics g) {
                // Draw everything in the game ArrayLists
                Graphics2D g2d = (Graphics2D)g;
                g2d.setColor(Color.white);

                if (players.size() > 0) {
                    for (Player p : players) {
                        for (Missile m: p.missiles) {
                            g2d.drawImage(m.getImage(), m.getX(), m.getY(), this);
                        }
                        g2d.drawImage(p.getImage(), p.getX(), p.getY(), this);
                        g2d.drawString(p.getName(), p.getX(), p.getY());
                    }
                    for (Enemy e : enemies) {
                        g2d.drawImage(e.getImage(), e.getX(), e.getY(), this);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
    
    static final int DEFAULT_WIDTH = 600;
    static final int DEFAULT_HEIGHT = 600;
    private static final String HOST_NAME = "localhost";
    private static final int PORT_NUM = 6969;
    private static final int PLAYER_START_X = 0;
    private static final int PLAYER_START_Y = 0;
}
