import com.pixault.auth.*;
import com.pixault.database.*;
import com.pixault.model.*;
import java.util.Optional;

public class TestLoginSuccess {
    public static void main(String[] args) throws Exception {
        DatabaseInitializer.initialize();
        UserDAO dao = new UserDAO();
        Optional<User> u = dao.findByUsernameOrEmail("TARUN");
        if (u.isPresent()) {
            User user = u.get();
            System.out.println("User ID: " + user.getId());
            AuthController auth = new AuthController();
            Session session = auth.loginSuccess(user);
            System.out.println("Session created: " + session.getSessionId());
        } else {
            System.out.println("User not found!");
        }
    }
}
