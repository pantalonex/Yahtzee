package auth;

import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Store {
  private final Path file;
  private static final int ITERATIONS = 100_000;
  private static final int KEY_LENGTH = 256;

  public Store(Path dataDir) throws IOException {
    Files.createDirectories(dataDir);
    this.file = dataDir.resolve("users.txt");
    if (Files.notExists(file)) {
        Files.createFile(file);
    }
  }

  public Store() throws IOException {
    this(Paths.get("data"));
  }

  // Register
  public synchronized boolean register(String username, String password) throws Exception {
    // Controllo username
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(username + ":")) return false;
      }
    }
    // Genera salt+hash
    byte[] salt = new byte[16];
    SecureRandom.getInstanceStrong().nextBytes(salt);
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                              .generateSecret(spec).getEncoded();
    String entry = username + ":" +
                   Base64.getEncoder().encodeToString(salt) + ":" +
                   Base64.getEncoder().encodeToString(hash);
    // Append
    try (BufferedWriter writer = Files.newBufferedWriter(file, 
            StandardOpenOption.APPEND)) {
      writer.write(entry);
      writer.newLine();
    }
    return true;
  }

  // Login 
  public synchronized boolean login(String username, String password) throws Exception {
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith(username + ":")) continue;
        String[] parts = line.split(":", 3);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] storedHash = Base64.getDecoder().decode(parts[2]);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                                      .generateSecret(spec).getEncoded();
        return Arrays.equals(hash, storedHash);
      }
    }
    return false;
  }
}
