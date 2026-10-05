import com.pixault.stego.StegoEngine;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;

public class TestStegoEngine {
    public static void main(String[] args) throws Exception {
        // 1. Create a dummy image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        // Fill it with white
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                img.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] originalImage = baos.toByteArray();
        
        String payload = "This is a super secret payload that must be hidden securely!";
        char[] password = "MySecurePassword123".toCharArray();
        
        System.out.println("Hiding data...");
        byte[] stegoImage = StegoEngine.encryptAndHide(originalImage, payload, password);
        System.out.println("Stego image size: " + stegoImage.length);
        
        System.out.println("Extracting data...");
        // Use a new char array to simulate fresh input
        char[] extractPassword = "MySecurePassword123".toCharArray();
        String extracted = StegoEngine.extractAndDecrypt(stegoImage, extractPassword);
        
        System.out.println("Extracted successfully: " + extracted.equals(payload));
        System.out.println("Payload: " + extracted);
    }
}
