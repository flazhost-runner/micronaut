package io.micronaut.http.uri;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UriTemplateRawPathTest {
    @Test
    void testGetRawPathComponent() {
        String uri = "/test/1.0/024-02-07T00:30:48.014+00:00";
        assertEquals("024-02-07T00:30:48.014+00:00", UriTemplate.getRawPathComponent(uri));
    }

    @Test
    void testGetRawPathComponentWithQuery() {
        String uri = "/test/1.0/024-02-07T00:30:48.014+00:00?param=value";
        assertEquals("024-02-07T00:30:48.014+00:00", UriTemplate.getRawPathComponent(uri));
    }
}
