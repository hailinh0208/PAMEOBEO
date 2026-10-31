package com.pameobeo.auth.infrastructure.security;

import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the RSA key pair used to sign JWTs (RS256), generating it on first start.
 * Only auth-service holds the private key; every other service verifies tokens with the public key
 * published at {@code /.well-known/jwks.json}.
 */
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAKey rsaKey;

    public RsaKeyProvider(Path keyDir, String keyId) {
        this.rsaKey = load(keyDir, keyId);
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey load(Path keyDir, String keyId) {
        try {
            Path privatePath = keyDir.resolve("private.key");
            Path publicPath = keyDir.resolve("public.key");
            KeyPair pair;
            if (Files.exists(privatePath) && Files.exists(publicPath)) {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                pair = new KeyPair(
                        factory.generatePublic(new X509EncodedKeySpec(decode(publicPath))),
                        factory.generatePrivate(new PKCS8EncodedKeySpec(decode(privatePath))));
                log.info("Loaded RSA signing key from {}", keyDir.toAbsolutePath());
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                pair = generator.generateKeyPair();
                Files.createDirectories(keyDir);
                Files.writeString(privatePath, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
                Files.writeString(publicPath, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
                log.info("Generated new RSA signing key in {}", keyDir.toAbsolutePath());
            }
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decode(Path path) throws IOException {
        return Base64.getDecoder().decode(Files.readString(path).trim());
    }
}
