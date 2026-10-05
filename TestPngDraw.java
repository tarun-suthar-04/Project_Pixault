import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestPngDraw {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        // Set a pixel to a specific value with an LSB set
        img.setRGB(0, 0, 0xFE123457); 
        
        System.out.println("Original RGB: " + Integer.toHexString(img.getRGB(0, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        BufferedImage readImg = ImageIO.read(new ByteArrayInputStream(bytes));
        System.out.println("Read RGB directly: " + Integer.toHexString(readImg.getRGB(0, 0)));

        BufferedImage converted = new BufferedImage(readImg.getWidth(), readImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
        converted.getGraphics().drawImage(readImg, 0, 0, null);

        System.out.println("Converted RGB via drawImage: " + Integer.toHexString(converted.getRGB(0, 0)));
        System.out.println("Are they exactly equal? " + (img.getRGB(0, 0) == converted.getRGB(0, 0)));
    }
}
