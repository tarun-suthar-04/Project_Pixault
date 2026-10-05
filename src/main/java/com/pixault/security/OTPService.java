package com.pixault.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OTP Service supporting:
 * <ul>
 * <li>TOTP (Time-based One-Time Password, RFC 6238) via Google
 * Authenticator</li>
 * <li>Email OTP (6-digit, 5-minute expiry, stored as SHA-256 hash)</li>
 * </ul>
 *
 * <p>
 * Email OTPs are stored as SHA-256 hashes to prevent exposure in case of DB
 * compromise.
 */
public final class OTPService {

    private static final Logger log = LoggerFactory.getLogger(OTPService.class);

    private static final int EMAIL_OTP_LENGTH = 6;
    private static final int EMAIL_OTP_EXPIRY_MINUTES = 5;
    private static final int TOTP_WINDOW = 1; // ±1 time-step tolerance
    private static final String DIGITS = "0123456789";

    private final GoogleAuthenticator googleAuthenticator;
    private final SecureRandom secureRandom;

    public OTPService() {
        this.googleAuthenticator = new GoogleAuthenticator();
        this.secureRandom = CryptoUtils.getSecureRandom();
    }

    // =========================================================================
    // TOTP (RFC 6238)
    // =========================================================================

    /**
     * Generates a new TOTP secret for a user.
     * The returned key should be stored encrypted in the database.
     *
     * @return Base32-encoded TOTP secret
     */
    public String generateTotpSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Generates a QR code URL (otpauth://) for use with authenticator apps.
     *
     * @param issuer  Application name (e.g., "Pixault")
     * @param account User's email or username
     * @param secret  Base32-encoded TOTP secret
     * @return OTP Auth URL for QR code generation
     */
    public String getTotpQRUrl(String issuer, String account, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, account,
                new GoogleAuthenticatorKey.Builder(secret).build());
    }

    /**
     * Verifies a TOTP code against the user's stored secret.
     * Allows ±1 time window to account for clock drift.
     *
     * @param secret  Base32-encoded TOTP secret
     * @param otpCode 6-digit code from authenticator app
     * @return true if code is valid within time window
     */
    public boolean verifyTotp(String secret, int otpCode) {
        try {
            return googleAuthenticator.authorize(secret, otpCode, TOTP_WINDOW);
        } catch (Exception e) {
            log.warn("TOTP verification error: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Email OTP
    // =========================================================================

    /**
     * Generates a secure 6-digit numeric OTP for email delivery.
     *
     * @return Raw OTP string (only show to user via email, never store raw)
     */
    public String generateEmailOtp() {
        StringBuilder otp = new StringBuilder(EMAIL_OTP_LENGTH);
        for (int i = 0; i < EMAIL_OTP_LENGTH; i++) {
            otp.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        }
        return otp.toString();
    }

    /**
     * Hashes an Email OTP for secure storage.
     * Store this hash in the DB, not the raw OTP.
     *
     * @param rawOtp Raw 6-digit OTP string
     * @return SHA-256 hex hash of the OTP
     */
    public String hashEmailOtp(String rawOtp) {
        return HashUtils.sha256Hex(rawOtp);
    }

    /**
     * Verifies a user-submitted OTP against the stored hash in constant time.
     *
     * @param submittedOtp OTP entered by user
     * @param storedHash   SHA-256 hash from database
     * @return true if OTP is correct
     */
    public boolean verifyEmailOtp(String submittedOtp, String storedHash) {
        if (submittedOtp == null || storedHash == null)
            return false;
        String computedHash = hashEmailOtp(submittedOtp);
        return HashUtils.constantTimeEquals(computedHash, storedHash);
    }

    /**
     * Returns the OTP expiry time from the current moment.
     */
    public LocalDateTime getEmailOtpExpiry() {
        return LocalDateTime.now().plusMinutes(EMAIL_OTP_EXPIRY_MINUTES);
    }

    /**
     * Checks if an OTP has expired.
     *
     * @param expiresAt Expiry timestamp from database
     * @return true if expired
     */
    public boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
