package com.aowss.m3u;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class PlaylistReader {

    public static Stream<String> streamFile(Path filePath) {

        try {

            var lineNumber = new AtomicLong();

            Stream<String> result = Files.lines(filePath, StandardCharsets.UTF_8)
                    .map(incrementLineNumber.apply(lineNumber))
                    .map(startsWithBOMCheck.apply(lineNumber))
                    .map(validLineCheck.apply(lineNumber))
                    .filter(not(emptyLine))
                    .filter(not(commentLine))
                    .map(validCharactersCheck.apply(lineNumber))
                    .map(normalizationCheck.apply(lineNumber));

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error while reading the file", e);
        }

    }

    private static Function<AtomicLong, Function<String, String>> incrementLineNumber = lineNumber -> line -> {
        lineNumber.getAndIncrement();
        return line;
    };

    private static Predicate<String> uri = line -> {
        try {
            new URI(line);
            return true;
        } catch (URISyntaxException use) {
            return false;
        }
    };
    private static Predicate<String> startsWithHash = line -> line.charAt(0) == '#';
    private static Predicate<String> emptyLine = line -> line.isBlank();
    public static Predicate<String> validLine = emptyLine.or(startsWithHash).or(uri);

    static Function<AtomicLong, Function<String, String>> validLineCheck = lineNumber -> line -> {
        if (!validLine.test(line)) throw new RuntimeException("Line " + lineNumber + " is invalid");
        return line;
    };

    static BiPredicate<AtomicLong, String> startsWithBOM = (lineNumber, line) -> lineNumber.longValue() == 1 && line.startsWith("\uFEFF");

    static Function<AtomicLong, Function<String, String>> startsWithBOMCheck = lineNumber -> line -> {
        if (startsWithBOM.test(lineNumber, line)) throw new RuntimeException("The file starts with a BOM");
        return line;
    };

    static Predicate<String> commentLine = line -> line.charAt(0) == '#' && ( line.length() < 4 || !line.startsWith("#EXT") );

    static int hexValue1 = 0x0000;
    static int hexValue2 = 0x001F;
    static int crValue = 0x000D;
    static int lfValue = 0x000A;
    static int hexValue3 = 0x007F;
    static int hexValue4 = 0x009F;

    public static Predicate<String> containInvalidCharacters = line -> line
            .codePoints()
            .filter(codePoint -> ( codePoint <= hexValue2 && codePoint != crValue && codePoint != lfValue ) || ( codePoint >= hexValue3 && codePoint <= hexValue4 ))
            .findAny()
            .isPresent();

    static Function<AtomicLong, Function<String, String>> validCharactersCheck = lineNumber -> line -> {
        if (containInvalidCharacters.test(line)) throw new RuntimeException("Line " + lineNumber + " contains one or more invalid characters");
        return line;
    };

    static Function<AtomicLong, Function<String, String>> normalizationCheck = lineNumber -> line -> {
        if (!Normalizer.isNormalized(line, Normalizer.Form.NFC)) throw new RuntimeException("Line " + lineNumber + " isn't NFC-normalized");
        return line;
    };

}
