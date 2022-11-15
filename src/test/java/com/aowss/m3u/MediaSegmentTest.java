package com.aowss.m3u;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Media Segment Parsing")
public class MediaSegmentTest {

    @Test
    @DisplayName("Missing EXTINF line")
    public void missingEXTINFTag() {
        Throwable exception = assertThrows(RuntimeException.class, () -> MediaSegment.parse(new Line(1, ""), new Line(2, "")));
        assertThat(exception.getMessage(), is("A media segment must have an EXTINF tag and a URI"));
    }

    @Test
    @DisplayName("Missing URI line")
    public void missingURI() {
        Throwable exception = assertThrows(RuntimeException.class, () -> MediaSegment.parse(new Line(1, "first"), new Line(2, "")));
        assertThat(exception.getMessage(), is("A media segment must have an EXTINF tag and a URI"));
    }

    @Test
    @DisplayName("No EXTINF tag")
    public void isEXTINFTag() {
        Throwable exception = assertThrows(RuntimeException.class, () -> MediaSegment.parse(new Line(1, "wrong"), new Line(2, "uri")));
        assertThat(exception.getMessage(), is("A media segment must start with an EXTINF tag"));
    }

    @Test
    @DisplayName("With attributes")
    public void attributes() throws URISyntaxException {
        Line line1 = new Line(1, "#EXTINF:-1 tvg-name=\"TFC\" tvg-id=\"None\" group-title=\"Filipino\",TFC");
        Line line2 = new Line(2, "http://vapi.vaders.tv/play/38782.m3u8?token=tokenValue");
        var mediaSegment = MediaSegment.parse(line1, line2);
        assertThat(mediaSegment.title(), is("TFC"));
        assertThat(mediaSegment.attributes(), is(Map.of(
                "tvg-name", "TFC",
                "tvg-id", "None",
                "group-title", "Filipino"
        )));
        assertThat(mediaSegment.uri(), is(new URI(line2.content())));
    }

    @Test
    void keyValue() {
        var input = "key=value";
        var result = MediaSegment.parseAttributes(input);
        assertThat(result, is(Map.of("key", "value")));
    }

    @Test
    void withSpaces() {
        var input = " key = value ";
        var result = MediaSegment.parseAttributes(input);
        assertThat(result, is(Map.of("key", "value")));
    }

    @Test
    void quoted() {
        var input = "key = \"value 1\" ";
        var result = MediaSegment.parseAttributes(input);
        assertThat(result, is(Map.of("key", "value 1")));
    }

    @Test
    void multiple() {
        var input = "key1=\"value 1\"  key2 =  value2 ";
        var result = MediaSegment.parseAttributes(input);
        assertThat(result, is(Map.of("key1", "value 1", "key2", "value2")));
    }

}
