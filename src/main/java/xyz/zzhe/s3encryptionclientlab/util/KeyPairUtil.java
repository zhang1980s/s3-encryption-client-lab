package xyz.zzhe.s3encryptionclientlab.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyPairUtil {

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public static void saveKeyPair(KeyPair keyPair, String publicKeyPath, String privateKeyPath) throws IOException {
        // Save public key
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyPEM = BEGIN_PUBLIC_KEY + "\n" +
                Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "\n" +
                END_PUBLIC_KEY;
        Files.write(Paths.get(publicKeyPath), publicKeyPEM.getBytes());

        // Save private key
        PrivateKey privateKey = keyPair.getPrivate();
        String privateKeyPEM = BEGIN_PRIVATE_KEY + "\n" +
                Base64.getEncoder().encodeToString(privateKey.getEncoded()) + "\n" +
                END_PRIVATE_KEY;
        Files.write(Paths.get(privateKeyPath), privateKeyPEM.getBytes());
    }

    public static KeyPair loadKeyPair(String publicKeyPath, String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Load public key
        String publicKeyPEM = new String(Files.readAllBytes(Path.of(publicKeyPath)));
        PublicKey publicKey = loadPublicKeyFromPEM(publicKeyPEM);

        // Load private key
        String privateKeyPEM = new String(Files.readAllBytes(Path.of(privateKeyPath)));
        PrivateKey privateKey = loadPrivateKeyFromPEM(privateKeyPEM);

        return new KeyPair(publicKey, privateKey);
    }

    public static KeyPair reconstructKeyPair(String publicKeyPEM, String privateKeyPEM) {
        try {
            PublicKey publicKey = loadPublicKeyFromPEM(publicKeyPEM);
            PrivateKey privateKey = loadPrivateKeyFromPEM(privateKeyPEM);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct key pair", e);
        }
    }

    private static PublicKey loadPublicKeyFromPEM(String publicKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyContent = publicKeyPEM
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return keyFactory.generatePublic(keySpec);
    }

    private static PrivateKey loadPrivateKeyFromPEM(String privateKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyContent = privateKeyPEM
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return keyFactory.generatePrivate(keySpec);
    }
}