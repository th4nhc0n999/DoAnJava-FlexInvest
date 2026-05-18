package Utils;

import java.security.SecureRandom;

/**
 * OtpUtils — sinh và xác minh mã OTP 6 chữ số.
 * Không cần SMTP thật — OTP được giả lập qua console/dialog.
 */
public class OtpUtils {

    private static final SecureRandom RNG = new SecureRandom();

    private OtpUtils() {}

    /**
     * Sinh mã OTP 6 chữ số (có thể có số 0 ở đầu).
     * @return chuỗi 6 ký tự số, ví dụ "047832"
     */
    public static String generate() {
        int code = RNG.nextInt(1_000_000);       // 0..999999
        return String.format("%06d", code);       // đảm bảo đủ 6 chữ số
    }

    /**
     * Kiểm tra OTP người dùng nhập có khớp với OTP đã sinh không.
     * So sánh case-insensitive và trim whitespace.
     *
     * @param input  OTP người dùng nhập
     * @param stored OTP đã sinh trước đó
     * @return true nếu khớp
     */
    public static boolean verify(String input, String stored) {
        if (input == null || stored == null) return false;
        return stored.trim().equals(input.trim());
    }
}
