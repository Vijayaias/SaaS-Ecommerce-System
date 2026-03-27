package com.javatechie.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "stripe_demo_secret";
    private static final long EXPIRATION = 1000L * 60 * 60 * 10;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractUsername(String token) {
        return extractClaim(token, claims -> (String) claims.get("sub"));
    }

    public Date extractExpiration(String token) {
        Number exp = extractClaim(token, claims -> (Number) claims.get("exp"));
        return Date.from(Instant.ofEpochSecond(exp.longValue()));
    }

    public <T> T extractClaim(String token, Function<Map<String, Object>, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Map<String, Object> extractAllClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!expectedSignature.equals(parts[2])) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            return objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        try {
            long nowMillis = System.currentTimeMillis();
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", username);
            claims.put("iat", nowMillis / 1000);
            claims.put("exp", (nowMillis + EXPIRATION) / 1000);

            String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
            String payloadJson = objectMapper.writeValueAsString(claims);

            String header = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT token", ex);
        }
    }

    private String sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }
}
