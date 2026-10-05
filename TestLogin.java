import com.pixault.security.KeyDerivation;

public class TestLogin {
    public static void main(String[] args) {
        try {
            System.out.println("Testing BouncyCastle init...");
            Class.forName("com.pixault.security.KeyDerivation");
            System.out.println("KeyDerivation class loaded.");

            System.out.println("Testing Argon2 hashing...");
            String hash = KeyDerivation.hashPasswordArgon2id(new char[]{'t','e','s','t'});
            System.out.println("Hash: " + hash);

            System.out.println("Testing Argon2 verification...");
            boolean verify = KeyDerivation.verifyArgon2id(hash, new char[]{'t','e','s','t'});
            System.out.println("Verify: " + verify);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
