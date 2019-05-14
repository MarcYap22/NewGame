import javax.swing.*;
import java.awt.*;

public class Sprite {
    int x, y;
    int width, height;
    Image image;

    Sprite (int x, int y) {
        this.x = x;
        this.y = y;
    }

    void loadImage(String filename) {
        ImageIcon ii = new ImageIcon(filename);
        image = ii.getImage();
    }

    void loadImageDimensions() {
        width = image.getWidth(null);
        height = image.getHeight(null);
    }

    public Image getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
