package com.keroles.wso2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Map;

@Component
public class Wso2ApiClient {

    static {
        // WSO2 serves self-signed certs signed by its own internal CA (client-truststore.jks).
        // Trust that CA for all HTTPS in this app; skip hostname check because the cert is CN=wso2carbon.
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream in = Wso2ApiClient.class.getResourceAsStream("/wso2-truststore.jks")) {
                ks.load(in, "wso2carbon".toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String tokenUrl;
    private final String apiUrl;
    private final String consumerKey;
    private final String consumerSecret;
    private final RestTemplate rest = new RestTemplate();

    public Wso2ApiClient(@Value("${wso2.token-url}") String tokenUrl,
                         @Value("${wso2.api-url}") String apiUrl,
                         @Value("${wso2.consumer-key}") String consumerKey,
                         @Value("${wso2.consumer-secret}") String consumerSecret) {
        this.tokenUrl = tokenUrl;
        this.apiUrl = apiUrl;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    public String getPosts() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(fetchToken());
        ResponseEntity<String> resp = rest.exchange(apiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return resp.getBody();
    }

    private String fetchToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(consumerKey, consumerSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        ResponseEntity<Map> resp = rest.postForEntity(tokenUrl, new HttpEntity<>(body, headers), Map.class);
        return (String) resp.getBody().get("access_token");
    }
}
