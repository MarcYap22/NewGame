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

    Image getImage() {
        return image;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

}
