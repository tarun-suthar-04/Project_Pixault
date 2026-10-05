package com.pixault.auth;

import com.pixault.database.OTPDAO;
import com.pixault.database.ShareDAO;
import com.pixault.database.UserDAO;
import com.pixault.model.AuditLog;
import com.pixault.model.Session;
import com.pixault.model.User;
import com.pixault.security.EmailService;
import com.pixault.security.HashUtils;
import com.pixault.security.KeyDerivation;
import com.pixault.security.OTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Central authentication controller implementing secure OTP-based flows and session management.
 */
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserDAO userDAO;
    private final ShareDAO shareDAO;
    private final OTPDAO otpDao;
    private final OTPService otpService;
    private final EmailService emailService;

    public AuthController() {
        this.userDAO = new UserDAO();
        this.shareDAO = new ShareDAO();
        this.otpDao = new OTPDAO();
        this.otpService = new OTPService();
        this.emailService = new EmailService();
    }

    /**
     * Registers a new user and initiates OTP verification.
     */
    public User register(String username, String email, char[] password)
            throws AuthException, SQLException {
        validateUsername(username);
        validateEmail(email);
        validatePasswordStrength(password);

        if (userDAO.findByUsernameOrEmail(username).isPresent()) {
            throw new AuthException("Username already taken.");
        }
        if (userDAO.findByUsernameOrEmail(email).isPresent()) {
            throw new AuthException("Email already registered.");
        }

        String hash = KeyDerivation.hashPasswordArgon2id(password);
        byte[] saltBytes = KeyDerivation.generateSalt();
        String saltBase64 = KeyDerivation.encodeSalt(saltBytes);

        User user = new User(username, email, hash, saltBase64);
        int userId = userDAO.createUser(user);

        initiateEmailVerification(user);

        logAudit(userId, AuditLog.REGISTER, null, DeviceFingerprint.generate(),
                "New user registered: " + username);
        return user;
    }

    public void initiateEmailVerification(User user) throws AuthException {
        try {
            String otp = otpService.generateEmailOtp();
            String hash = otpService.hashEmailOtp(otp);
            otpDao.insertOtp(user.getId(), hash, "EMAIL_VERIFY", otpService.getEmailOtpExpiry());
            emailService.sendVerificationOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            log.error("Failed to send verification OTP", e);
            throw new AuthException("Could not send verification email. Please try again from Login.");
        }
    }

    public void verifyEmailOtp(User user, String submittedOtp) throws AuthException, SQLException {
        Optional<OTPDAO.OtpRecord> opt = otpDao.findLatestValidOtp(user.getId(), "EMAIL_VERIFY");
        if (opt.isEmpty() || opt.get().isExpired()) {
            throw new AuthException("OTP expired or invalid. Please request a new one.");
        }

        OTPDAO.OtpRecord record = opt.get();
        if (record.attempts >= 5) {
            throw new AuthException("Too many failed attempts. Please request a new OTP.");
        }

        if (!otpService.verifyEmailOtp(submittedOtp, record.otpHash)) {
            otpDao.incrementAttempts(record.id);
            throw new AuthException("Invalid OTP code.");
        }

        userDAO.markEmailVerified(user.getId());
        otpDao.markConsumed(record.id);
        logAudit(user.getId(), AuditLog.EMAIL_VERIFIED, null, null, "Email verified via OTP");
    }

    public void initiateForgotPassword(String email) throws AuthException, SQLException {
        Optional<User> optUser = userDAO.findByUsernameOrEmail(email);
        if (optUser.isEmpty()) {
            return;
        }

        User user = optUser.get();
        try {
            String otp = otpService.generateEmailOtp();
            String hash = otpService.hashEmailOtp(otp);
            otpDao.insertOtp(user.getId(), hash, "FORGOT_PASSWORD", otpService.getEmailOtpExpiry());
            emailService.sendForgotPasswordOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            log.error("Failed to send forgot password OTP", e);
        }
    }

    public void resetPasswordWithOtp(String email, String otp, char[] newPassword) 
            throws AuthException, SQLException {
        validatePasswordStrength(newPassword);
        Optional<User> optUser = userDAO.findByUsernameOrEmail(email);
        if (optUser.isEmpty()) throw new AuthException("Invalid request.");

        User user = optUser.get();
        Optional<OTPDAO.OtpRecord> optOtp = otpDao.findLatestValidOtp(user.getId(), "FORGOT_PASSWORD");
        
        if (optOtp.isEmpty() || optOtp.get().isExpired()) {
            throw new AuthException("OTP expired or invalid.");
        }

        OTPDAO.OtpRecord record = optOtp.get();
        if (!otpService.verifyEmailOtp(otp, record.otpHash)) {
            otpDao.incrementAttempts(record.id);
            throw new AuthException("Invalid OTP.");
        }

        String newHash = KeyDerivation.hashPasswordArgon2id(newPassword);
        byte[] newSalt = KeyDerivation.generateSalt();
        userDAO.updatePasswordHash(user.getId(), newHash, KeyDerivation.encodeSalt(newSalt));
        
        otpDao.markConsumed(record.id);
        SessionManager.invalidateAllSessions(user.getId());
        
        try {
            emailService.sendSecurityAlertEmail(user.getEmail(), "Your Pixault password was changed successfully.");
        } catch (Exception e) {
            log.warn("Failed to send security alert email", e);
        }

        logAudit(user.getId(), AuditLog.PASSWORD_RESET, null, null, "Password reset via OTP");
    }

    public User authenticatePassword(String identifier, char[] password)
            throws AuthException, SQLException {
        Optional<User> optUser = userDAO.findByUsernameOrEmail(identifier);
        if (optUser.isEmpty()) {
            KeyDerivation.hashPasswordArgon2id(new char[] { 'd', 'u', 'm', 'm', 'y' });
            throw new AuthException("Invalid username or password.");
        }

        User user = optUser.get();

        if (user.isLocked()) {
            throw new AuthException("Account is locked until " + user.getLoginLockedUntil()
                    + ". Please try again later.");
        }

        boolean valid = KeyDerivation.verifyArgon2id(user.getPasswordHash(), password);
        if (!valid) {
            handleFailedLoginAttempt(user);
            throw new AuthException("Invalid username or password.");
        }

        userDAO.resetFailedLoginAttempts(user.getId());
        return user;
    }

    public Session loginSuccess(User user) throws AuthException, SQLException {
        if (!user.isEmailVerified()) {
            throw new AuthException("EMAIL_UNVERIFIED");
        }

        String fingerprint = DeviceFingerprint.generate();
        Session session = SessionManager.createSession(user.getId(), user.getUsername(), fingerprint);

        logAudit(user.getId(), AuditLog.LOGIN_SUCCESS, null, fingerprint, "Login successful");
        return session;
    }

    private void handleFailedLoginAttempt(User user) throws SQLException {
        userDAO.incrementFailedLoginAttempts(user.getId());
        int newCount = user.getFailedLoginAttempts() + 1;
        if (newCount >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            userDAO.lockLogin(user.getId(), lockUntil);
            logAudit(user.getId(), AuditLog.ACCOUNT_LOCKED, null, null,
                    "Account locked after " + newCount + " failed attempts");
        } else {
            logAudit(user.getId(), AuditLog.LOGIN_FAILURE, null, null,
                    "Failed attempt " + newCount + "/" + MAX_FAILED_ATTEMPTS);
        }
    }

    private void logAudit(Integer userId, String event, String ip, String device, String details) {
        try {
            shareDAO.insertAuditLog(new AuditLog(userId, event, ip, device, details));
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }

    public void changePassword(User user, char[] currentPassword, char[] newPassword)
            throws AuthException, SQLException {
        validatePasswordStrength(newPassword);
        if (!KeyDerivation.verifyArgon2id(user.getPasswordHash(), currentPassword)) {
            throw new AuthException("Current password is incorrect.");
        }
        String newHash = KeyDerivation.hashPasswordArgon2id(newPassword);
        byte[] newSalt = KeyDerivation.generateSalt();
        userDAO.updatePasswordHash(user.getId(), newHash, KeyDerivation.encodeSalt(newSalt));
        SessionManager.invalidateAllSessions(user.getId());
        logAudit(user.getId(), AuditLog.PASSWORD_CHANGED, null, DeviceFingerprint.generate(),
                "Password changed - all sessions invalidated");
    }

    private void validateUsername(String username) throws AuthException {
        if (username == null || username.length() < 3 || username.length() > 64) {
            throw new AuthException("Username must be 3-64 characters.");
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new AuthException("Username may only contain letters, digits, and underscores.");
        }
    }

    private void validateEmail(String email) throws AuthException {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new AuthException("Invalid email address.");
        }
    }

    private void validatePasswordStrength(char[] password) throws AuthException {
        if (password == null || password.length < 12) {
            throw new AuthException("Password must be at least 12 characters.");
        }
    }

    public boolean isEmailVerified(String identifier) throws SQLException {
        return userDAO.findByUsernameOrEmail(identifier)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    public static class AuthException extends Exception {
        public AuthException(String message) { super(message); }
        public AuthException(String message, Throwable cause) { super(message, cause); }
    }
}
