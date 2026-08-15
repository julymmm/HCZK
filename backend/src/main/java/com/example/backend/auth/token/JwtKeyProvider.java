package com.example.backend.auth.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT RSA 密钥提供器。
 *
 * <p>生产环境应通过配置注入固定私钥和公钥。私钥只用于签发 token，公钥只用于验签。
 * 如果本地没有配置密钥，会生成一组临时密钥，方便开发，但应用重启后旧 token 会全部失效。</p>
 */
@Component
public class JwtKeyProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtKeyProvider(@Value("${app.jwt.private-key:}") String privateKeyValue,
                          @Value("${app.jwt.public-key:}") String publicKeyValue) {
        if (StringUtils.hasText(privateKeyValue) && StringUtils.hasText(publicKeyValue)) {
            this.privateKey = parsePrivateKey(privateKeyValue);
            this.publicKey = parsePublicKey(publicKeyValue);
        } else {
            KeyPair keyPair = generateDevelopmentKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
            log.warn("JWT RSA key pair is generated in memory. Configure app.jwt.private-key and app.jwt.public-key for stable production tokens.");
        }
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    private static PrivateKey parsePrivateKey(String value) {
        try {
            byte[] bytes = decodePemOrBase64(value, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT private key", e);
        }
    }

    private static PublicKey parsePublicKey(String value) {
        try {
            byte[] bytes = decodePemOrBase64(value, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT public key", e);
        }
    }

    private static byte[] decodePemOrBase64(String value, String type) {
        String normalized = value.replace("\\n", "\n").trim();
        normalized = normalized
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private static KeyPair generateDevelopmentKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT RSA key pair", e);
        }
    }
}