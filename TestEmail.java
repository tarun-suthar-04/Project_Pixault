import com.pixault.security.EmailService;
import com.pixault.config.AppConfig;

public class TestEmail {
    public static void main(String[] args) {
        try {
            System.out.println("Init config...");
            System.setProperty("mail.debug", "true");
            
            System.out.println("Init EmailService...");
            EmailService emailService = new EmailService();
            
            System.out.println("Calling sendVerificationOtpEmail...");
            try {
                emailService.sendVerificationOtpEmail("test@example.com", "123456");
                System.out.println("Email sent!");
            } catch (Exception e) {
                System.out.println("Caught exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
