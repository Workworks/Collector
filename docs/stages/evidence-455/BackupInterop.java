import com.kfaino.collecter.core.EncryptedBackup;
import java.nio.file.*;
public class BackupInterop {
  public static void main(String[] args) throws Exception {
    char[] password="public-qa-password-455".toCharArray();
    if(args[0].equals("encrypt")) {
      String json="{\"entries\":[{\"id\":\"interop-desktop\",\"brand\":\"跨端加密样例\"}],\"saved_searches\":[{\"id\":\"query\",\"query\":\"样例\"}]}";
      Files.write(Path.of(args[1]),EncryptedBackup.INSTANCE.encrypt(json,password));
      System.out.println("JVM fixture created (public QA data only)");
    } else {
      String json=EncryptedBackup.INSTANCE.decrypt(Files.readAllBytes(Path.of(args[1])),password);
      if(!json.contains("interop-desktop") || !json.contains("android-verified")) throw new AssertionError("Cross-platform fields missing");
      System.out.println("PASS: JVM encrypt -> Android decrypt/edit/encrypt -> JVM decrypt");
    }
    java.util.Arrays.fill(password,'\0');
  }
}