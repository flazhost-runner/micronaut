/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.uri;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * Most defensive approach to URL encoding and decoding.
 * <p>
 * Rules determined by combining the unreserved character set from
 * <a href="https://www.rfc-editor.org/rfc/rfc3986#page-13">RFC 3986</a> with
 * the percent-encode set from
 * <a href="https://url.spec.whatwg.org/#application-x-www-form-urlencoded-percent-encode-set">application/x-www-form-urlencoded</a>.
 * <p>
 * Both specs above support percent decoding of two hexadecimal digits to a
 * binary octet, however their unreserved set of characters differs and
 * {@code application/x-www-form-urlencoded} adds conversion of space to +,
 * which has the potential to be misunderstood.
 * <p>
 * This class encodes with rules that will be decoded correctly in either case.
 *
 * <p>Forked from https://github.com/gbevin/urlencoder/blob/main/src/main/java/com/uwyn/urlencoder/UrlEncoder.java</p>
 *
 * @since 4.8.0
 */
final class RFC3986UrlEncoder {
    static final BitSet UNRESERVED_URI_CHARS;
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    static {
        // see https://www.rfc-editor.org/rfc/rfc3986#page-13
        // and https://url.spec.whatwg.org/#application-x-www-form-urlencoded-percent-encode-set
        var unreserved = new BitSet('z' + 1);
        unreserved.set('-');
        unreserved.set('.');
        for (int c = '0'; c <= '9'; ++c) unreserved.set(c);
        for (int c = 'A'; c <= 'Z'; ++c) unreserved.set(c);
        unreserved.set('_');
        for (int c = 'a'; c <= 'z'; ++c) unreserved.set(c);
        UNRESERVED_URI_CHARS = unreserved;
    }

    private RFC3986UrlEncoder() {
        // no-op
    }

    /**
     * Transforms a provided <code>String</code> object into a new string,
     * containing only valid URL characters in the UTF-8 encoding.
     *
     * @param source The string that has to be transformed into a valid URL
     *               string.
     * @return The encoded <code>String</code> object.
     * @since 1.0
     */
    static String encode(String source) {
        return encode(source, null, false);
    }

    /**
     * Transforms a provided <code>String</code> object into a new string,
     * containing only valid URL characters in the UTF-8 encoding.
     *
     * @param source The string that has to be transformed into a valid URL
     *               string.
     * @param allow  Additional characters to allow.
     * @return The encoded <code>String</code> object.
     * @since 1.0
     */
    static String encode(String source, String allow) {
        return encode(source, allow, false);
    }

    /**
     * Transforms a provided <code>String</code> object into a new string,
     * containing only valid URL characters in the UTF-8 encoding.
     *
     * @param source      The string that has to be transformed into a valid URL
     *                    string.
     * @param spaceToPlus Convert any space to {@code +}.
     * @return The encoded <code>String</code> object.
     * @since 1.0
     */
    static String encode(String source, boolean spaceToPlus) {
        return encode(source, null, spaceToPlus);
    }

    /**
     * Transforms a provided <code>String</code> object into a new string,
     * containing only valid URL characters in the UTF-8 encoding.
     *
     * @param source      The string that has to be transformed into a valid URL
     *                    string.
     * @param allow       Additional characters to allow.
     * @param spaceToPlus Convert any space to {@code +}.
     * @return The encoded <code>String</code> object.
     * @since 1.0
     */
    static String encode(String source, String allow, boolean spaceToPlus) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        StringBuilder out = null;
        char ch;
        var i = 0;
        while (i < source.length()) {
            ch = source.charAt(i);
            if (isUnreservedUriChar(ch) || (allow != null && allow.indexOf(ch) != -1)) {
                if (out != null) {
                    out.append(ch);
                }
                i += 1;
            } else {
                out = startConstructingIfNeeded(out, source, i);

                var cp = source.codePointAt(i);
                if (cp < 0x80) {
                    if (spaceToPlus && ch == ' ') {
                        out.append('+');
                    } else {
                        appendUrlEncodedByte(out, cp);
                    }
                    i += 1;
                } else if (Character.isBmpCodePoint(cp)) {
                    for (var b : Character.toString(ch).getBytes(StandardCharsets.UTF_8)) {
                        appendUrlEncodedByte(out, b);
                    }
                    i += 1;
                } else if (Character.isSupplementaryCodePoint(cp)) {
                    var high = Character.highSurrogate(cp);
                    var low = Character.lowSurrogate(cp);
                    for (var b : new String(new char[]{high, low}).getBytes(StandardCharsets.UTF_8)) {
                        appendUrlEncodedByte(out, b);
                    }
                    i += 2;
                }
            }
        }

        if (out == null) {
            return source;
        }

        return out.toString();
    }

    // see https://www.rfc-editor.org/rfc/rfc3986#page-13
    // and https://url.spec.whatwg.org/#application-x-www-form-urlencoded-percent-encode-set

    private static boolean isUnreservedUriChar(char ch) {
        return ch <= 'z' && UNRESERVED_URI_CHARS.get(ch);
    }

    private static void appendUrlEncodedByte(StringBuilder out, int ch) {
        out.append("%");
        appendUrlEncodedDigit(out, ch >> 4);
        appendUrlEncodedDigit(out, ch);
    }

    private static void appendUrlEncodedDigit(StringBuilder out, int digit) {
        out.append(HEX_DIGITS[digit & 0x0F]);
    }

    private static StringBuilder startConstructingIfNeeded(StringBuilder out, String source, int currentSourcePosition) {
        if (out == null) {
            out = new StringBuilder(source.length());
            out.append(source, 0, currentSourcePosition);
        }
        return out;
    }
}
