package auth;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.*;
import java.util.Base64;

public class CryptoUtils {
    private static KeyPair rsaKeyPair;
    private SecretKey aesKey;
    private IvParameterSpec iv;

    public CryptoUtils() throws Exception {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(256);
        this.aesKey = gen.generateKey();

        byte[] ivBytes = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(ivBytes);
        this.iv = new IvParameterSpec(ivBytes);
    }

    public CryptoUtils(byte[] aesKeyBytes, byte[] ivBytes) {
        this.aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
        this.iv     = new IvParameterSpec(ivBytes);
    }

    public SecretKey getAesKey() {
        return aesKey;
    }

    public void setAesKey(SecretKey key) {
        aesKey = key;
    }

    public static void initRSA() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        rsaKeyPair = gen.generateKeyPair();
    }

    public static PublicKey getPublicKey() {
        return rsaKeyPair.getPublic();
    }

    public static String encryptRSA(String data, PublicKey pk) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        byte[] enc = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(enc);
    }


    public static String decryptRSA(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
        byte[] dec = cipher.doFinal(Base64.getDecoder().decode(data));
        return new String(dec);
    }


    public void initAES() throws Exception {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(256);
        aesKey = gen.generateKey();
        byte[] ivBytes = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(ivBytes);
        iv = new IvParameterSpec(ivBytes);
    }

    public String encryptAES(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] enc = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(iv.getIV()) + ":" + Base64.getEncoder().encodeToString(enc);
    }

    public String decryptAES(String data) throws Exception {
        String[] parts = data.split(":",2);
        IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(parts[0]));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] dec = cipher.doFinal(Base64.getDecoder().decode(parts[1]));
        return new String(dec);
    }

    public IvParameterSpec getIv(){ 
        return iv; 
    }
    
    public void setIv(IvParameterSpec ivSpec){ 
        iv = ivSpec; 
    }
}
