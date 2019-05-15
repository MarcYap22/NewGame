import java.util.concurrent.CopyOnWriteArrayList;

class Player extends Sprite {

    CopyOnWriteArrayList<Missile> missiles = new CopyOnWriteArrayList<>();

    private String name;

    int firingRate = 100;
    int firingCounter;

    boolean isMovingUp;
    boolean isMovingDown;
    boolean isMovingRight;
    boolean isMovingLeft;

    boolean isFiring;

    private int speed = 10;

    Player (int x, int y) {
        super(x, y);

        loadImage(FILENAME);
        loadImageDimensions();
    }

    void move() {
        int dx, dy;

        if (isMovingUp) {
            dy = -speed;
        } else if (isMovingDown){
            dy = speed;
        }
        else {
            dy = 0;
        }


        if (isMovingLeft){
            dx = -speed;
        } else if (isMovingRight){
            dx = speed;
        } else {
            dx = 0;
        }

        x += dx;
        y += dy;
    }


    void fire() {
        int midX = x + width/2;
        missiles.add(new Missile(midX, y));
    }

    void addMissile(Missile m) {
        missiles.add(m);
    }

    void setName(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    private static final String FILENAME = "player.png";
}
