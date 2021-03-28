package com.aowss.m3u;

import com.aowss.m3u.Line;
import com.aowss.m3u.PlaylistReader;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.aowss.m3u.PlaylistReader.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Playlists rules from https://tools.ietf.org/html/rfc8216")
public class PlaylistReaderTest {

    static String notUTF8Encoded                = "not-utf8.m3u";
    static String UTF8WithBOM                   = "utf8-with-bom.m3u";
    static String UTF8WithControlCharacters     = "control-character.m3u";
    static String UTF8NotNFCNormalized          = "not-nfc-normalized.m3u";

    static String sample                        = "sample.m3u";
    static String sampleWithBlankLines          = "with-blank-lines.m3u";
    static String sampleWithCommentLines        = "with-comment-lines.m3u";

    @Test
    @Tag("UTF-8")
    @DisplayName("Playlist files MUST be encoded in UTF-8")
    public void isUTF8Encoded() throws URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(notUTF8Encoded).toURI());
        Throwable exception = assertThrows(UncheckedIOException.class, () -> PlaylistReader.streamFile(path).count());
        assertThat(exception.getCause(), Matchers.instanceOf(MalformedInputException.class));
    }

    @Test
    @Tag("UTF-8")
    @DisplayName("Playlist files MUST NOT contain any Byte Order Mark (BOM)")
    public void noBOM() throws URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(UTF8WithBOM).toURI());
        Throwable exception = assertThrows(RuntimeException.class, () -> PlaylistReader.streamFile(path).count());
        assertThat(exception.getMessage(), is("The file starts with a BOM"));
    }

    @Test
    @Tag("UTF-8")
    @DisplayName("Playlist files MUST NOT contain UTF-8 control characters (U+0000 to U+001F and U+007F to U+009F), with the exceptions of CR (U+000D) and LF (U+000A)")
    public void noUTF8ControlCharacters() throws URISyntaxException {

        String invalid = "test\tthat";
        assertThat(containInvalidCharacters.test(new Line(1, invalid)), is(true));

        String valid = "test\nthat";
        assertThat(containInvalidCharacters.test(new Line(2, valid)), is(false));

        Path path = Paths.get(getClass().getClassLoader().getResource(UTF8WithControlCharacters).toURI());
        Throwable exception = assertThrows(RuntimeException.class, () -> PlaylistReader.streamFile(path).count());
        assertThat(exception.getMessage(), is("Line 8 contains one or more invalid characters"));

    }

    @Test
    @Tag("UTF-8")
    @DisplayName("All character sequences MUST be normalized according to Unicode normalization form \"NFC\"")
    public void normalizedSequences() throws URISyntaxException {

        Path validPath = Paths.get(getClass().getClassLoader().getResource(sample).toURI());
        assertThat(PlaylistReader.streamFile(validPath).count(), is(10L));

        Path invalidPath = Paths.get(getClass().getClassLoader().getResource(UTF8NotNFCNormalized).toURI());
        Throwable exception = assertThrows(RuntimeException.class, () -> PlaylistReader.streamFile(invalidPath).count());
        assertThat(exception.getMessage(), is("Line 5 isn't NFC-normalized"));

    }

    @Test
    @Tag("Line")
    @DisplayName("Lines in a Playlist file are terminated by either a single line feed character or a carriage return character followed by a line feed character")
    public void properLineTermination() {
        //  There is nothing to do here !
    }

    @Test
    @Tag("Line")
    @DisplayName("Each line is a URI, is blank, or starts with the character '#'")
    public void validLine() {

        String blank = "";
        assertThat(validLine.test(new Line(1, blank)), is(true));

        String hashLine = "# comment";
        assertThat(validLine.test(new Line(1, hashLine)), is(true));

        String tagLine = "#EXT tag";
        assertThat(validLine.test(new Line(1, tagLine)), is(true));

        String uriLine = "http://media.example.com/first.ts";
        assertThat(validLine.test(new Line(1, uriLine)), is(true));

        String invalidLine = "this is not a uri";
        assertThat(validLine.test(new Line(1, invalidLine)), is(false));

    }

    @Test
    @Tag("Line")
    @DisplayName("Blank lines are ignored")
    public void blankLines() throws URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(sampleWithBlankLines).toURI());
        assertThat(PlaylistReader.streamFile(path).count(), is(10L));
    }

    @Test
    @Tag("Line")
    @DisplayName("All other lines that begin with '#' are comments and SHOULD be ignored")
    public void commentLines() throws URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(sampleWithCommentLines).toURI());
        assertThat(PlaylistReader.streamFile(path).count(), is(10L));
    }

    //  Whitespace MUST NOT be present, except for elements in which it is explicitly specified

}
