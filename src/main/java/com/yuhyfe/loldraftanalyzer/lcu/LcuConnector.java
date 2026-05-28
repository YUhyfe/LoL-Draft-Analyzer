package com.yuhyfe.loldraftanalyzer.lcu;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class LcuConnector {
    private static LcuConnector instance;

    private String port;
    private String password;

    public LcuConnector() throws IOException {
        readLockFile();
    }

    public static LcuConnector getInstance() {
        if (instance == null) {
            try {
                instance = new LcuConnector();
            } catch (IOException e) {
                throw new RuntimeException("Cannot read lockfile. Is League client running?", e);
            }
        }
        return instance;
    }

    public void readLockFile() throws IOException {
        Path path = Path.of("C:/Riot Games/League of Legends/lockfile");

        String[] content = Files.readString(path).split(":");
        this.port = content[2];
        this.password = Base64.getEncoder().encodeToString(("riot:" + content[3]).getBytes());
    }

    public String get(String endpoint) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        }, new SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://127.0.0.1:" + port + endpoint))
                .header("Authorization", "Basic " + password)
                .GET()
                .build();

        String url = "https://127.0.0.1:" + port + endpoint;
        System.out.println("URL: " + url);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
