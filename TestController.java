import com.pixault.auth.AuthController;
import com.pixault.database.DBConnection;
import com.pixault.database.DatabaseInitializer;
import com.pixault.config.AppConfig;

public class TestController {
    public static void main(String[] args) {
        try {
            System.out.println("Init config...");
            
            System.out.println("Init DB...");
            DatabaseInitializer.initialize();
            
            System.out.println("Init AuthController...");
            AuthController auth = new AuthController();
            
            System.out.println("Calling authenticatePassword...");
            try {
                auth.authenticatePassword("TARUN", new char[]{'t','e','s','t','1','2','3'});
                System.out.println("Returned cleanly!");
            } catch (Exception e) {
                System.out.println("Caught exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
