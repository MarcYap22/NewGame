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

    // Game lists to mirror those in the Server
    private CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();

    private Client() {
        // Connect to the server,
        // instantiate the IO streams,
        // then start the sender and receiver threads
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
     * Sends the playerActions to the Server.
     *
     * PLAYER ACTIONS FORMAT:
     *
     *  [Player_Name] [up] [down] [left] [right] [fire]
     *
     *  Player_Name is a String, the rest are booleans.
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

        /*
         Always start by sending the name
         Then just keep sending the playerActions
          */
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
     * Receives data from the Server and processes it
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
     * Parses the data string and updates the game variables.
     *
     * TEST FORMAT (without missiles)
     * - without the extra newlines
     * - separated with spaces
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
     *
     * @param data String that contains the game state.
     */
    private void processData(String data) {
        Scanner in = new Scanner(data);
        enemies.clear();
        players.clear();
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.equals("PLAYERS")) {
                int numPlayers = Integer.parseInt(in.nextLine());
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
            }
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

                        if (k == KeyEvent.VK_W) {
                            player.isMovingUp = true;
                        } else if (k == KeyEvent.VK_A) {
                            player.isMovingLeft = true;
                        } else if (k == KeyEvent.VK_S) {
                            player.isMovingDown = true;
                        } else if (k == KeyEvent.VK_D) {
                            player.isMovingRight = true;
                        }
                    }


                    @Override
                    public void keyReleased(KeyEvent e) {
                        int k = e.getKeyCode();

                        if (k == KeyEvent.VK_W) {
                            player.isMovingUp = false;
                        } else if (k == KeyEvent.VK_A) {
                            player.isMovingLeft = false;
                        } else if (k == KeyEvent.VK_S) {
                            player.isMovingDown = false;
                        } else if (k == KeyEvent.VK_D) {
                            player.isMovingRight = false;
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

                // start the game
                while (true) {
                    moveObjects();
                    try {
                        Thread.sleep(GAME_DELAY);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            void moveObjects() {
                for (Player p: players) {
                    p.move();
                }

                for (Enemy e: enemies) {
                    e.move();
                }

                repaint();
            }


            @Override
            protected void paintComponent(Graphics g) {
                // Draw everything in the game ArrayLists
                Graphics2D g2d = (Graphics2D)g;
                for (Player p: players) {
                    g2d.drawImage(p.getImage(), p.getX(), p.getY(), this);
                    g2d.setColor(Color.white);
                    g2d.drawString(p.getName(), p.getX(), p.getY());
                    g2d.setColor(Color.black);
                }
                for (Enemy e: enemies) {
                    g2d.drawImage(e.getImage(), e.getX(), e.getY(), this);
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
