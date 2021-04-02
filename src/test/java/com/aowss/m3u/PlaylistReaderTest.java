package com.aowss.m3u;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@DisplayName("Playlists rules from https://tools.ietf.org/html/rfc8216#section-4")
public class PlaylistReaderTest {

    private static WireMockServer wireMockServer;

    static String wrongExtension                = "sample.m4u";

    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @Test
    @Tag("Path")
    @DisplayName("The file path must end with either .m3u8 or .m3u")
    public void path() throws URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(wrongExtension).toURI());
        Throwable exception = assertThrows(RuntimeException.class, () -> PlaylistReader.fromFile.apply(path));
        assertThat(exception.getMessage(), is("The path must end with either .m3u8 or .m3u"));
    }

    @Test
    @Tag("URL")
    @DisplayName("If the URI ends with either .m3u8 or .m3u, the Content-Type is irrelvant")
    public void url() throws URISyntaxException, IOException {
        wireMockServer.stubFor(
            get(urlEqualTo("/playlist/sample.m3u"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(readFileContent("sample.m3u"))
                )
        );

        wireMockServer.stubFor(
            get(urlEqualTo("/playlist/sample.m3u8"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/vnd.apple.mpegurl")
                        .withBody(readFileContent("sample.m3u"))
                )
        );

        assertThat(PlaylistReader.fromURI.apply(new URI("http://localhost:8090/playlist/sample.m3u")).length(), is(10L));
        assertThat(PlaylistReader.fromURI.apply(new URI("http://localhost:8090/playlist/sample.m3u8")).length(), is(10L));
    }

    @Test
    @Tag("URL")
    @DisplayName("If the URI path doesn't end with either .m3u8 or .m3u, the Content-Type must be either 'application/vnd.apple.mpegurl' or 'audio/mpegurl'")
    public void contentType() throws URISyntaxException, IOException {
        wireMockServer.stubFor(
            get(urlEqualTo("/playlist/wrong"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(readFileContent("sample.m3u"))
                )
        );

        wireMockServer.stubFor(
            get(urlEqualTo("/playlist/right/1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/vnd.apple.mpegurl")
                        .withBody(readFileContent("sample.m3u"))
                )
        );

        wireMockServer.stubFor(
            get(urlEqualTo("/playlist/right/2"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "audio/mpegurl")
                        .withBody(readFileContent("sample.m3u"))
                )
        );

        Throwable exception = assertThrows(RuntimeException.class, () -> PlaylistReader.fromURI.apply(new URI("http://localhost:8090/playlist/wrong")));
        assertThat(exception.getMessage(), is("The URI must end with either .m3u8 or .m3u or the Content-Type must be either 'application/vnd.apple.mpegurl' or 'audio/mpegurl'"));

        assertThat(PlaylistReader.fromURI.apply(new URI("http://localhost:8090/playlist/right/1")).length(), is(10L));
        assertThat(PlaylistReader.fromURI.apply(new URI("http://localhost:8090/playlist/right/2")).length(), is(10L));
    }

    public static String readFileContent(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get("src", "test", "resources", path)), "UTF-8");
    }

}
