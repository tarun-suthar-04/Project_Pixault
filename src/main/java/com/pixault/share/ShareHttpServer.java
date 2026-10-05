package com.pixault.share;

import com.pixault.auth.AuthController;
import com.pixault.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Lightweight local HTTP server for one-time share links.
 *
 * Endpoint:
 *   GET /share?id=<shareId>
 */
public final class ShareHttpServer {

    private static final Logger log = LoggerFactory.getLogger(ShareHttpServer.class);

    private static HttpServer server;

    private ShareHttpServer() {
    }

    public static synchronized void startIfConfigured() {
        if (server != null) {
            return;
        }

        try {
            Properties config = AppConfig.snapshot();
            String baseUrl = config.getProperty("share.public.url",
                    config.getProperty("app.base.url", "http://localhost:8088"));
            URI uri = URI.create(baseUrl);

            String host = config.getProperty("share.bind.host", "0.0.0.0").trim();
            if (host.isBlank()) {
                host = "0.0.0.0";
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 8088;

            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            ShareService shareService = new ShareService();
            AuthController authController = new AuthController();

            server.createContext("/share", exchange -> handleShare(exchange, shareService));
            server.createContext("/health", exchange -> writeText(exchange, 200, "Pixault Share Server is running"));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            log.info("Share HTTP server started at {}:{}", host, port);
        } catch (Exception e) {
            log.warn("Share HTTP server not started: {}", e.getMessage());
            server = null;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            log.info("Share HTTP server stopped.");
        }
    }

    private static void handleShare(HttpExchange exchange, ShareService shareService) throws IOException {
        String method = exchange.getRequestMethod();
        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            writeText(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String shareId = query.get("id");
        if (shareId == null || shareId.isBlank()) {
            writeText(exchange, 400, "Missing share id");
            return;
        }

        boolean openRequested = "1".equals(query.getOrDefault("open", ""));
        if (!openRequested) {
            String html = buildShareLandingPage(shareId.trim());
            writeHtml(exchange, 200, html);
            return;
        }

        try {
            ShareService.ShareAccessResult result = shareService.accessShare(shareId.trim());
            String html = buildSharePage(result);
            writeHtml(exchange, 200, html);
        } catch (ShareService.ShareException e) {
            writeHtml(exchange, 410, buildShareErrorPage(e.getMessage()));
        } catch (SQLException e) {
            writeHtml(exchange, 500, buildShareErrorPage("Database error: " + e.getMessage()));
        } catch (Exception e) {
            writeHtml(exchange, 500, buildShareErrorPage("Share access failed: " + e.getMessage()));
        }
    }

    private static void writeText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    private static String buildSharePage(ShareService.ShareAccessResult result) {
        String message = result.getDecryptedMessage();
        if (message == null || message.isBlank()) {
            message = "No decrypted message was attached to this share.";
        }
        message = escapeHtml(message);

        return "<html><head><meta charset='utf-8'><title>Pixault Share</title>"
                + "<style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;background:#0f142b;color:#e5e7eb;padding:24px;}"
                + ".card{max-width:920px;margin:auto;background:#1d2342;border:1px solid #334155;border-radius:14px;padding:20px;}"
                + "h2{margin-top:0;color:#a78bfa;}"
                + "pre{white-space:pre-wrap;background:#111827;border:1px solid #334155;border-radius:10px;padding:14px;color:#d1fae5;}"
                + ".note{color:#fca5a5;font-size:13px;margin-top:12px;}"
                + "</style></head><body>"
                + "<div class='card'><h2>Pixault One-Time Share</h2>"
                + "<p>Decrypted message:</p><pre>" + message + "</pre>"
                + "<p>Shared image preview is disabled by sender settings.</p>"
                + "<p class='note'>This link is one-time use and is now consumed.</p>"
                + "</div></body></html>";
    }

    private static String buildShareLandingPage(String shareId) {
        String safeShareId = escapeHtml(shareId);
        String openUrl = "/share?id=" + safeShareId + "&open=1";
        return "<html><head><meta charset='utf-8'><title>Pixault Share</title>"
                + "<meta name='robots' content='noindex,nofollow'>"
                + "<style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;background:#0f142b;color:#e5e7eb;padding:24px;}"
                + ".card{max-width:680px;margin:auto;background:#1d2342;border:1px solid #334155;border-radius:14px;padding:24px;text-align:center;}"
                + "h2{margin-top:0;color:#a78bfa;} p{line-height:1.5;color:#cbd5e1;}"
                + ".btn{display:inline-block;margin-top:12px;background:#7c3aed;color:#fff;text-decoration:none;padding:12px 22px;border-radius:10px;font-weight:700;}"
                + ".note{margin-top:14px;font-size:13px;color:#fca5a5;}"
                + "</style></head><body>"
                + "<div class='card'><h2>Pixault Secure Share</h2>"
                + "<p>This is a one-time secure link.</p>"
                + "<p>Tap the button below to open shared content.</p>"
                + "<a class='btn' href='" + openUrl + "'>OPEN SHARED CONTENT</a>"
                + "<p class='note'>Previews in chat apps will not consume the link.</p>"
                + "</div></body></html>";
    }

    private static String buildShareErrorPage(String message) {
        return "<html><head><meta charset='utf-8'><title>Pixault Share</title>"
                + "<style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;background:#0f142b;color:#e5e7eb;padding:24px;}"
                + ".card{max-width:680px;margin:auto;background:#1d2342;border:1px solid #334155;border-radius:14px;padding:24px;}"
                + "h2{margin-top:0;color:#f87171;} p{line-height:1.5;color:#e5e7eb;}"
                + "</style></head><body>"
                + "<div class='card'><h2>Unable to Open Share</h2>"
                + "<p>" + escapeHtml(message) + "</p>"
                + "</div></body></html>";
    }

    private static String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
