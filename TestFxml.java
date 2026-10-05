import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import com.pixault.Main;

public class TestFxml {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                System.out.println("Loading VerifyEmail.fxml...");
                Parent root = FXMLLoader.load(Main.class.getResource("/fxml/VerifyEmail.fxml"));
                System.out.println("Loaded VerifyEmail.fxml: " + (root != null));
                
                System.out.println("Loading Dashboard.fxml...");
                Parent root2 = FXMLLoader.load(Main.class.getResource("/fxml/Dashboard.fxml"));
                System.out.println("Loaded Dashboard.fxml: " + (root2 != null));
                
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
