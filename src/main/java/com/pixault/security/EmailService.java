package com.pixault.security;

import com.pixault.config.AppConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Secure email delivery service for Pixault.
 *
 * <p>
 * Sends:
 * <ul>
 * <li>Email verification links (HMAC-signed, 15-minute expiry)</li>
 * <li>Password reset links (HMAC-signed, 10-minute expiry)</li>
 * <li>Email OTP codes (6-digit, 5-minute expiry)</li>
 * </ul>
 *
 * <p>
 * All tokens are HMAC-SHA256 signed and time-bound.
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final int EMAIL_VERIFY_TTL_MINUTES = 15;
    private static final int PASSWORD_RESET_TTL_MINUTES = 10;

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;
    private final boolean smtpAuth;
    private final boolean smtpStarttls;
    private final boolean smtpSsl;
    private final int smtpConnectionTimeoutMs;
    private final int smtpTimeoutMs;
    private final int smtpWriteTimeoutMs;
    private final boolean smtpDebug;
    private final String fromAddress;
    private final String baseUrl;
    private final byte[] hmacKey;

    public EmailService() {
        Properties config = AppConfig.snapshot();
        this.smtpHost = config.getProperty("mail.host", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(config.getProperty("mail.port", "587"));
        this.smtpUser = config.getProperty("mail.username", "");
        this.smtpPassword = config.getProperty("mail.password", "");
        this.smtpAuth = Boolean.parseBoolean(config.getProperty("mail.auth", "true"));
        this.smtpStarttls = Boolean.parseBoolean(config.getProperty("mail.starttls", "true"));
        this.smtpSsl = Boolean.parseBoolean(config.getProperty("mail.ssl", "false"));
        this.smtpConnectionTimeoutMs = parseInt(config, "mail.connectiontimeout.ms", 10000);
        this.smtpTimeoutMs = parseInt(config, "mail.timeout.ms", 10000);
        this.smtpWriteTimeoutMs = parseInt(config, "mail.writetimeout.ms", 10000);
        this.smtpDebug = Boolean.parseBoolean(config.getProperty("mail.debug", "false"));
        this.fromAddress = config.getProperty("mail.from", "Pixault <noreply@pixault.com>");
        this.baseUrl = resolveBaseUrl(config);
        String secret = AppConfig.get("token.hmac.secret", "CHANGE_ME_USE_A_LONG_RANDOM_SECRET");
        this.hmacKey = secret.getBytes(StandardCharsets.UTF_8);

        log.info("SMTP config loaded: host={}, port={}, auth={}, starttls={}, ssl={}, timeoutMs={}/{}/{}, baseUrl={}",
                smtpHost, smtpPort, smtpAuth, smtpStarttls, smtpSsl,
                smtpConnectionTimeoutMs, smtpTimeoutMs, smtpWriteTimeoutMs, baseUrl);
    }

    // =========================================================================
    // OTP & Security Emails
    // =========================================================================

    /**
     * Sends a 6-digit verification OTP code to the user.
     */
    public void sendVerificationOtpEmail(String toEmail, String otpCode) throws MessagingException {
        String subject = "🔐 Pixault – Verify Your Email Address";
        String body = buildOtpEmailBody("Email Verification", 
                "Use the code below to verify your account and activate your vault.", 
                otpCode);
        sendEmail(toEmail, subject, body);
        log.info("Verification OTP sent to {}", maskEmail(toEmail));
    }

    /**
     * Sends a 6-digit password reset OTP code.
     */
    public void sendForgotPasswordOtpEmail(String toEmail, String otpCode) throws MessagingException {
        String subject = "🔐 Pixault – Password Reset OTP";
        String body = buildOtpEmailBody("Password Reset", 
                "You requested to reset your password. Use the code below to proceed.", 
                otpCode);
        sendEmail(toEmail, subject, body);
        log.info("Password reset OTP sent to {}", maskEmail(toEmail));
    }

    /**
     * Sends an alert when the password is changed.
     */
    public void sendSecurityAlertEmail(String toEmail, String message) throws MessagingException {
        String subject = "🔐 Pixault – Security Alert";
        String body = buildEmailBody("Security Update", message, "Open Pixault", baseUrl, 
                "If you did not make this change, please contact security immediately.");
        sendEmail(toEmail, subject, body);
        log.info("Security alert sent to {}", maskEmail(toEmail));
    }

    private String buildOtpEmailBody(String title, String description, String otpCode) {
        return "<html><body style='font-family:Arial;background:#0f172a;color:#f8fafc;padding:30px'>"
                + "<div style='max-width:480px;margin:auto;background:#1e293b;border-radius:16px;padding:32px;"
                + "border:1px solid #38bdf8;text-align:center'>"
                + "<h2 style='color:#38bdf8;margin-bottom:8px'>🔐 PIXAULT</h2>"
                + "<h3 style='color:#f8fafc;margin-bottom:20px'>" + title + "</h3>"
                + "<p style='color:#94a3b8;margin-bottom:24px'>" + description + "</p>"
                + "<div style='font-size:36px;letter-spacing:10px;color:#38bdf8;background:#0f172a;"
                + "padding:20px;border-radius:12px;font-weight:bold;margin-bottom:24px;border:1px dashed #334155'>"
                + otpCode + "</div>"
                + "<p style='color:#64748b;font-size:13px'>This code expires in 5 minutes. Do not share this code with anyone.</p>"
                + "</div></body></html>";
    }

    /**
     * Validates a signed token (checks HMAC signature, expiry, type).
     *
     * @param rawToken     Token string from URL
     * @param expectedType "EMAIL_VERIFY" or "PASSWORD_RESET"
     * @return userId embedded in token, or -1 on invalid/expired
     */
    public int verifyEmailToken(String rawToken, String expectedType) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(rawToken);
            String tokenStr = new String(decoded, StandardCharsets.UTF_8);
            // Format: userId:email:type:expiryEpoch:hmac
            String[] parts = tokenStr.split(":");
            if (parts.length < 5)
                return -1;

            int userId = Integer.parseInt(parts[0]);
            String type = parts[2];
            long expiryEpoch = Long.parseLong(parts[3]);
            String hmac = parts[4];

            if (!expectedType.equals(type))
                return -1;
            if (System.currentTimeMillis() > expiryEpoch)
                return -1;

            // Verify HMAC
            String dataToVerify = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
            if (!HashUtils.verifyHmac(dataToVerify.getBytes(StandardCharsets.UTF_8), hmac, hmacKey)) {
                return -1;
            }
            return userId;
        } catch (Exception e) {
            log.warn("Token verification failed: {}", e.getMessage());
            return -1;
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private String generateSignedToken(int userId, String email, String type, int ttlMinutes) {
        long expiryEpoch = System.currentTimeMillis() + (ttlMinutes * 60 * 1000L);
        String data = userId + ":" + email + ":" + type + ":" + expiryEpoch;
        String hmac = HashUtils.hmacSha256Hex(data.getBytes(StandardCharsets.UTF_8), hmacKey);
        String fullToken = data + ":" + hmac;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(fullToken.getBytes(StandardCharsets.UTF_8));
    }

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStarttls));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtpSsl));
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.user", smtpUser);
        props.put("mail.smtp.user", smtpUser);
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpConnectionTimeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(smtpTimeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpWriteTimeoutMs));

        Session session = smtpAuth
                ? Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPassword);
                    }
                })
                : Session.getInstance(props);
        session.setDebug(smtpDebug);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");
        try (Transport transport = session.getTransport("smtp")) {
            if (smtpAuth) {
                transport.connect(smtpHost, smtpPort, smtpUser, smtpPassword);
            } else {
                transport.connect();
            }
            transport.sendMessage(message, message.getAllRecipients());
        }
    }

    private int parseInt(Properties props, String key, int fallback) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for {}: {}. Using fallback={}", key, raw, fallback);
            return fallback;
        }
    }

    private String resolveBaseUrl(Properties config) {
        String configured = config.getProperty("app.base.url", "http://localhost:8088").trim();
        if (configured.isBlank()) {
            configured = "http://localhost:8088";
        }

        try {
            URI uri = URI.create(configured);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return trimTrailingSlash(configured);
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                String localIp = detectLocalIpv4();
                if (localIp != null) {
                    URI rebuilt = new URI(
                            uri.getScheme(),
                            uri.getUserInfo(),
                            localIp,
                            uri.getPort(),
                            uri.getPath(),
                            null,
                            null);
                    String url = trimTrailingSlash(rebuilt.toString());
                    log.info("app.base.url points to localhost; using LAN URL {}", url);
                    return url;
                }
            }
            return trimTrailingSlash(configured);
        } catch (Exception e) {
            log.warn("Invalid app.base.url '{}': {}. Falling back to http://localhost:8088",
                    configured, e.getMessage());
            return "http://localhost:8088";
        }
    }

    private String detectLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not detect LAN IP for email links: {}", e.getMessage());
        }
        return null;
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildEmailBody(String title, String body, String btnText, String btnUrl, String footer) {
        return "<html><body style='font-family:Arial;background:#121212;color:#fff;padding:30px'>"
                + "<div style='max-width:560px;margin:auto;background:#1E1E2E;border-radius:16px;"
                + "padding:36px;border:1px solid #7C3AED'>"
                + "<h2 style='color:#7C3AED;text-align:center;margin-bottom:4px'>🔐 Pixault</h2>"
                + "<h3 style='text-align:center;color:#E2E8F0'>" + title + "</h3>"
                + "<p style='color:#CBD5E1;text-align:center'>" + body + "</p>"
                + "<div style='text-align:center;margin:28px 0'>"
                + "<a href='" + btnUrl + "' style='background:linear-gradient(135deg,#7C3AED,#4F46E5);"
                + "color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;"
                + "font-weight:bold;font-size:15px'>" + btnText + "</a></div>"
                + "<p style='color:#64748B;font-size:12px;text-align:center'>" + footer + "</p>"
                + "</div></body></html>";
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        return (local.length() > 2 ? local.substring(0, 2) + "***" : "**") + "@" + parts[1];
    }

}
