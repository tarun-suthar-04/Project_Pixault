import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class TestPngIndexed {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED);
        img.setRGB(0, 0, 0xFF123456);
        System.out.println("Set RGB: " + Integer.toHexString(0xFF123456));
        System.out.println("Got RGB: " + Integer.toHexString(img.getRGB(0, 0)));
    }
}
