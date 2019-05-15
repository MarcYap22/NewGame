public class Enemy extends Sprite {


    int dy = 5;
    int dx = 0;


    Enemy(int x, int y) {
        super(x, y);

        loadImage(FILENAME);
        loadImageDimensions();
    }


    void move() {
        x += dx;
        y += dy;
    }

    public void setDx(int dx) {
        this.dx = dx;
    }

    public void setDy(int dy) {
        this.dy = dy;
    }

    private final String FILENAME = "enemy.png";

}
