import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestPngSetRgb {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        int argb = (0 << 24) | (12 << 16) | (34 << 8) | 56;
        img.setRGB(0, 0, argb);
        
        System.out.println("Original RGB: " + Integer.toHexString(img.getRGB(0, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        BufferedImage readImg = ImageIO.read(new ByteArrayInputStream(bytes));
        System.out.println("Read Image Type: " + readImg.getType());
        System.out.println("Read RGB: " + Integer.toHexString(readImg.getRGB(0, 0)));
        
        // modify LSB
        int rArgb = readImg.getRGB(0, 0);
        int r = (rArgb >> 16) & 0xFF;
        r = (r & 0xFE) | 1; // set LSB of R to 1
        int newArgb = (rArgb & 0xFF00FFFF) | (r << 16);
        readImg.setRGB(0, 0, newArgb);
        System.out.println("Modified RGB: " + Integer.toHexString(readImg.getRGB(0, 0)));
        
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        ImageIO.write(readImg, "PNG", baos2);
        
        BufferedImage readImg2 = ImageIO.read(new ByteArrayInputStream(baos2.toByteArray()));
        System.out.println("Read modified RGB: " + Integer.toHexString(readImg2.getRGB(0, 0)));
    }
}
