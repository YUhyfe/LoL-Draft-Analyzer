package com.yuhyfe.loldraftanalyzer.lcu;

import com.yuhyfe.loldraftanalyzer.AppSettings;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Logger;

public class LcuConnector {

    private static final Logger LOG = AppLogger.get(LcuConnector.class);

    private static volatile LcuConnector instance;

    private final String     port;
    private final String     authHeader;
    private final HttpClient http;

    private LcuConnector() throws IOException {
        java.nio.file.Path lockPath = AppSettings.get().getLockfilePath();
        String[] parts = Files.readString(lockPath).split(":");
        this.port       = parts[2];
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(("riot:" + parts[3]).getBytes());
        this.http       = buildHttpClient();
        LOG.fine("LcuConnector initialised on port " + port);
    }

    public static LcuConnector getInstance() {
        if (instance == null) {
            synchronized (LcuConnector.class) {
                if (instance == null) {
                    try {
                        instance = new LcuConnector();
                    } catch (IOException e) {
                        throw new RuntimeException("Cannot read LCU lockfile. Is the League client running?", e);
                    }
                }
            }
        }
        return instance;
    }

    public static void reset() {
        instance = null;
        LOG.fine("LcuConnector reset");
    }

    public String get(String endpoint) throws Exception {
        String url = "https://127.0.0.1:" + port + endpoint;
        LOG.fine("LCU GET " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.fine("LCU -> " + response.statusCode());
        return response.body();
    }

    private static HttpClient buildHttpClient() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers()             { return null; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            }, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ctx).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCU HTTP client", e);
        }
    }
}
