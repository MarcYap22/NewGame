class Missile extends Sprite {
    int dy = -10;

    Missile(int x, int y) {
        super(x, y);

        loadImage(FILENAME);
        loadImageDimensions();
    }

    void move() {
        y += dy;
    }


    private static final String FILENAME = "missile.png";
}
