import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestPng {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFF123456); // Set a pixel

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        BufferedImage readImg = ImageIO.read(new ByteArrayInputStream(bytes));
        System.out.println("Written Type: " + img.getType());
        System.out.println("Read Type: " + readImg.getType());
        System.out.println("Are they same type? " + (img.getType() == readImg.getType()));
        System.out.println("Is Read TYPE_INT_ARGB? " + (readImg.getType() == BufferedImage.TYPE_INT_ARGB));
    }
}
