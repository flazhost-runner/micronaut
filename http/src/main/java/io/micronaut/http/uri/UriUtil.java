package io.micronaut.http.uri;

import java.net.URI;

public final class UriUtil {
    private UriUtil() {
    }

    /**
     * Transform a path+query as specified by the whatwg url spec into a path+query that is allowed
     * by RFC 3986. Whatwg permits certain characters (e.g. '|') and invalid percent escape
     * sequences that RFC 3986 (or {@link URI}) does not allow. This method will percent-encode
     * those cases, so that any URI sent by a browser can be transformed to {@link URI}.
     *
     * @param path The whatwg path+query
     * @return A valid RFC 3986 {@code relative-ref}
     */
    public static String toValidPath(String path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length();) {
            int cp = path.codePointAt(i);
            if (cp == '%') {
                boolean validEscape;
                if (i + 2 >= path.length()) {
                    validEscape = false;
                } else {
                    char c1 = path.charAt(i + 1);
                    char c2 = path.charAt(i + 2);
                    validEscape = isAsciiHexDigit(c1) && isAsciiHexDigit(c2);
                }
                if (validEscape) {
                    sb.appendCodePoint(cp);
                } else {
                    PercentEncoder.appendEncodedByte(sb, (byte) '%');
                }
            } else {
                if (cp == '/' && sb.length() == 1 && sb.charAt(0) == '/') {
                    // prevent '//' at start of url
                } else {
                    PercentEncoder.RFC3986_QUERY_CHAR.encodeUtf8(sb, cp);
                }
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }



    private static boolean isAsciiDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiUpperHexDigit(int c) {
        return isAsciiDigit(c) || (c >= 'A' && c <= 'F');
    }

    private static boolean isAsciiLowerHexDigit(int c) {
        return isAsciiDigit(c) || (c >= 'a' && c <= 'f');
    }

    private static boolean isAsciiHexDigit(int c) {
        return isAsciiLowerHexDigit(c) || isAsciiUpperHexDigit(c);
    }
}
