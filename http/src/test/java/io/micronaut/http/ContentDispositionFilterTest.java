package io.micronaut.http;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for verifying the behavior of the Content-Disposition header filter.
 */
@MicronautTest
public class ContentDispositionFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    /**
     * Tests that the Content-Disposition header is set correctly for the /report/result endpoint.
     */
    @Test
    void testContentDispositionHeader() {
        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/report/result"), String.class
        );

        assertEquals(200, response.getStatus().getCode());

        assertTrue(response.getHeaders().contains("Content-Disposition"));
        assertEquals(
            "attachment; filename=report.csv",
            response.getHeaders().get("Content-Disposition")
        );
    }
}

