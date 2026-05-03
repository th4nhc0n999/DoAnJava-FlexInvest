package Utils;

import java.util.Base64;
import java.util.UUID;

public class TokenUtils {

    public static String generate(int accountId) {
        String raw = accountId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
