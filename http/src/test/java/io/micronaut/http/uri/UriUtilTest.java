package io.micronaut.http.uri;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.junit.jupiter.api.Assertions;

import java.net.URI;
import java.net.URISyntaxException;

class UriUtilTest {
    @FuzzTest(maxDuration = "30m")
    void whatwgUrlCanBeFixedUp(FuzzedDataProvider data) throws URISyntaxException {
        String input = data.consumeRemainingAsAsciiString();
        WhatwgParser parser = new WhatwgParser(input);
        parser.setBaseUrl(new WhatwgUrl("http", "", "", "example.com", null, "/", false, null, null));
        try {
            parser.parse();
        } catch (IllegalArgumentException e) {
            return;
        }
        WhatwgUrl url = parser.toUrl();
        StringBuilder builder = new StringBuilder();
        builder.append(url.path);
        if (url.query != null) {
            builder.append("?").append(url.query);
        }
        String valid = UriUtil.toValidPath(builder.toString());
        URI uri = new URI(valid); // should not throw
        Assertions.assertEquals(url.query == null, uri.getRawQuery() == null);
    }
}
