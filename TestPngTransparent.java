import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestPngTransparent {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        // Set alpha to 0, but set RGB to some values (12, 34, 56)
        int argb = (0 << 24) | (12 << 16) | (34 << 8) | 56;
        img.setRGB(0, 0, argb);
        
        System.out.println("Original RGB: " + Integer.toHexString(img.getRGB(0, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        BufferedImage readImg = ImageIO.read(new ByteArrayInputStream(bytes));
        System.out.println("Read RGB: " + Integer.toHexString(readImg.getRGB(0, 0)));
        System.out.println("Are they equal? " + (img.getRGB(0, 0) == readImg.getRGB(0, 0)));
        
        // Also test converting back to TYPE_INT_ARGB
        BufferedImage converted = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        converted.getGraphics().drawImage(readImg, 0, 0, null);
        System.out.println("Converted RGB: " + Integer.toHexString(converted.getRGB(0, 0)));
    }
}
