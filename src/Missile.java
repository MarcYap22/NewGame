class Missile extends Sprite {
    int dy = -10;
    int dx = 0;

    Missile(int x, int y) {
        super(x, y);

        loadImage(FILENAME);
        loadImageDimensions();
    }

    void move() {
        y += dy;
        x += dx;
    }


    private static final String FILENAME = "missile.png";
}
