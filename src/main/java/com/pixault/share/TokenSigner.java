package com.pixault.share;

import com.pixault.security.HashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Signs and verifies compact HMAC-SHA256 tokens (similar to JWT but without
 * external library).
 *
 * <p>
 * Token format:
 * 
 * <pre>
 *   Base64URL(JSON claims) + "." + Base64URL(HMAC-SHA256(Base64URL(claims)))
 * </pre>
 *
 * <p>
 * Verification uses constant-time comparison to prevent timing attacks.
 */
public final class TokenSigner {

    private static final Logger log = LoggerFactory.getLogger(TokenSigner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TokenSigner() {
        throw new UnsupportedOperationException("TokenSigner is a utility class.");
    }

    /**
     * Signs a set of claims with HMAC-SHA256.
     *
     * @param claims    Key-value claims map (must be JSON-serializable)
     * @param secretKey HMAC secret key bytes
     * @return Signed token string
     * @throws TokenException on serialization failure
     */
    public static String sign(Map<String, String> claims, byte[] secretKey) throws TokenException {
        try {
            String jsonClaims = MAPPER.writeValueAsString(claims);
            String encodedClaims = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonClaims.getBytes(StandardCharsets.UTF_8));

            byte[] hmac = HashUtils.hmacSha256(
                    encodedClaims.getBytes(StandardCharsets.UTF_8), secretKey);
            String encodedHmac = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hmac);

            return encodedClaims + "." + encodedHmac;
        } catch (Exception e) {
            throw new TokenException("Token signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a signed token and returns its claims.
     *
     * @param token     Token string from {@link #sign}
     * @param secretKey HMAC secret key bytes
     * @return Claims map if valid
     * @throws TokenException if signature is invalid or token is malformed
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> verify(String token, byte[] secretKey) throws TokenException {
        if (token == null || !token.contains(".")) {
            throw new TokenException("Malformed token.");
        }
        try {
            int dotIndex = token.lastIndexOf('.');
            String encodedClaims = token.substring(0, dotIndex);
            String encodedHmac = token.substring(dotIndex + 1);

            // Recompute HMAC
            byte[] expectedHmac = HashUtils.hmacSha256(
                    encodedClaims.getBytes(StandardCharsets.UTF_8), secretKey);
            byte[] actualHmac = Base64.getUrlDecoder().decode(encodedHmac);

            // Constant-time comparison
            if (!HashUtils.constantTimeEquals(expectedHmac, actualHmac)) {
                throw new TokenException("Token signature verification failed.");
            }

            // Decode and parse claims
            byte[] jsonBytes = Base64.getUrlDecoder().decode(encodedClaims);
            return MAPPER.readValue(jsonBytes, Map.class);
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException("Token verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts claims from a token WITHOUT verifying the signature.
     * Use only for display/debugging – never trust unverified claims.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> unsafePeek(String token) {
        try {
            String encodedClaims = token.substring(0, token.lastIndexOf('.'));
            byte[] jsonBytes = Base64.getUrlDecoder().decode(encodedClaims);
            return MAPPER.readValue(jsonBytes, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static class TokenException extends Exception {
        public TokenException(String message) {
            super(message);
        }

        public TokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
