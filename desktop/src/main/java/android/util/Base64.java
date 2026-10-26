package android.util;

import java.nio.charset.StandardCharsets;

/**
 * Android's Base64, with its flags: lines of 76 characters ended by a line break unless NO_WRAP
 * (the last one included), and a lenient decoder skipping every character outside the alphabet.
 */
public final class Base64 {
    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;
    public static final int NO_CLOSE = 16;

    private static final int LINE_LENGTH = 76;

    private Base64() {
    }

    public static byte[] decode(String str, int flags) {
        return decode(str.getBytes(StandardCharsets.US_ASCII), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        return decode(input, 0, input.length, flags);
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        boolean urlSafe = (flags & URL_SAFE) != 0;
        StringBuilder clean = new StringBuilder(len);
        for (int i = offset; i < offset + len; i++) {
            char c = (char) (input[i] & 0xff);
            if (c == '=') break;
            if (inAlphabet(c, urlSafe)) clean.append(c);
        }
        if (clean.length() % 4 == 1) throw new IllegalArgumentException("bad base-64");
        java.util.Base64.Decoder decoder = urlSafe ? java.util.Base64.getUrlDecoder() : java.util.Base64.getDecoder();
        try {
            return decoder.decode(clean.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bad base-64", e);
        }
    }

    private static boolean inAlphabet(char c, boolean urlSafe) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) return true;
        return urlSafe ? (c == '-' || c == '_') : (c == '+' || c == '/');
    }

    public static String encodeToString(byte[] input, int flags) {
        return new String(encode(input, flags), StandardCharsets.US_ASCII);
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        return new String(encode(input, offset, len, flags), StandardCharsets.US_ASCII);
    }

    public static byte[] encode(byte[] input, int flags) {
        return encode(input, 0, input.length, flags);
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        byte[] slice = new byte[len];
        System.arraycopy(input, offset, slice, 0, len);
        java.util.Base64.Encoder encoder = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();
        if ((flags & NO_PADDING) != 0) encoder = encoder.withoutPadding();
        String encoded = encoder.encodeToString(slice);
        if ((flags & NO_WRAP) != 0 || encoded.isEmpty()) {
            return encoded.getBytes(StandardCharsets.US_ASCII);
        }
        String lineBreak = (flags & CRLF) != 0 ? "\r\n" : "\n";
        StringBuilder wrapped = new StringBuilder(encoded.length() + encoded.length() / LINE_LENGTH * 2 + 2);
        for (int i = 0; i < encoded.length(); i += LINE_LENGTH) {
            wrapped.append(encoded, i, Math.min(encoded.length(), i + LINE_LENGTH)).append(lineBreak);
        }
        return wrapped.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
