package com.pixault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

/**
 * Central application configuration loader.
 *
 * <p>Load order, from lowest to highest priority:
 * <ol>
 * <li>Bundled {@code /config.properties}</li>
 * <li>Local ignored {@code config.local.properties}</li>
 * <li>Environment variables, e.g. {@code PIXAULT_DB_PASSWORD}</li>
 * </ol>
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties PROPERTIES = load();

    private AppConfig() {
    }

    public static Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(PROPERTIES);
        return copy;
    }

    public static String get(String key, String fallback) {
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return PROPERTIES.getProperty(key, fallback);
    }

    public static int getInt(String key, int fallback) {
        String raw = get(key, Integer.toString(fallback));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for {}: {}. Using fallback={}", key, raw, fallback);
            return fallback;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, Boolean.toString(fallback)));
    }

    private static Properties load() {
        Properties props = new Properties();
        loadClasspathDefaults(props);
        loadLocalOverrides(props, Path.of("config.local.properties"));
        loadLocalOverrides(props, Path.of("config", "config.local.properties"));
        ensureRuntimeSecret(props);
        return props;
    }

    private static void loadClasspathDefaults(Properties props) {
        try (InputStream is = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                log.warn("Bundled config.properties not found. Using code defaults.");
            }
        } catch (IOException e) {
            log.warn("Could not load bundled config.properties: {}", e.getMessage());
        }
    }

    private static void loadLocalOverrides(Properties props, Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
            log.info("Loaded local configuration overrides from {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not load local config {}: {}", path.toAbsolutePath(), e.getMessage());
        }
    }

    private static String toEnvKey(String key) {
        return "PIXAULT_" + key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static void ensureRuntimeSecret(Properties props) {
        String envSecret = System.getenv(toEnvKey("token.hmac.secret"));
        if (envSecret != null && !envSecret.isBlank()) {
            return;
        }

        String configured = props.getProperty("token.hmac.secret", "");
        if (!isPlaceholderSecret(configured)) {
            return;
        }

        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        props.setProperty("token.hmac.secret", Base64.getEncoder().encodeToString(random));
        log.warn("token.hmac.secret is not configured. Using an ephemeral runtime secret for this launch.");
    }

    private static boolean isPlaceholderSecret(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("CHANGE_ME")
                || normalized.startsWith("REPLACE_WITH")
                || normalized.contains("DEFAULT_SECRET");
    }
}
