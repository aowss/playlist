package com.aowss.m3u;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class Validator {

    private static Function<AtomicLong, Function<String, Line>> addLineNumber = lineNumber -> line -> new Line(lineNumber.getAndIncrement(), line);

    private static Predicate<Line> uri = line -> {
        try {
            new URI(line.content());
            return true;
        } catch (URISyntaxException use) {
            return false;
        }
    };
    private static Predicate<Line> startsWithHash = line -> line.content().charAt(0) == '#';
    private static Predicate<Line> emptyLine = line -> line.content().isBlank();
    public static Predicate<Line> validLine = emptyLine.or(startsWithHash).or(uri);

    static UnaryOperator<Line> validLineCheck = line -> {
        if (!validLine.test(line)) throw new RuntimeException("Line " + line.lineNumber() + " is invalid");
        return line;
    };

    static Predicate<Line> startsWithEXTM3U = line -> line.lineNumber() != 1 ||line.content().equals("#EXTM3U");

    static UnaryOperator<Line> startsEXTM3UCheck = line -> {
        if (!startsWithEXTM3U.test(line)) throw new RuntimeException("The file must start with the EXTM3U tag");
        return line;
    };

    static Predicate<Line> startsWithBOM = line -> line.lineNumber() == 1 && line.content().startsWith("\uFEFF");

    static UnaryOperator<Line> notStartWithBOMCheck = line -> {
        if (startsWithBOM.test(line)) throw new RuntimeException("The file starts with a BOM");
        return line;
    };

    static Predicate<Line> commentLine = line -> line.content().charAt(0) == '#' && ( line.content().length() < 4 || !line.content().startsWith("#EXT") );

    static int hexValue1 = 0x0000;
    static int hexValue2 = 0x001F;
    static int crValue = 0x000D;
    static int lfValue = 0x000A;
    static int hexValue3 = 0x007F;
    static int hexValue4 = 0x009F;

    static Predicate<Line> containInvalidCharacters = line -> line.content()
            .codePoints()
            .filter(codePoint -> ( codePoint <= hexValue2 && codePoint != crValue && codePoint != lfValue ) || ( codePoint >= hexValue3 && codePoint <= hexValue4 ))
            .findAny()
            .isPresent();

    static UnaryOperator<Line> validCharactersCheck = line -> {
        if (containInvalidCharacters.test(line)) throw new RuntimeException("Line " + line.lineNumber() + " contains one or more invalid characters");
        return line;
    };

    static UnaryOperator<Line> normalizationCheck = line -> {
        if (!Normalizer.isNormalized(line.content(), Normalizer.Form.NFC)) throw new RuntimeException("Line " + line.lineNumber() + " isn't NFC-normalized");
        return line;
    };

    public static Function<Stream<String>, Stream<Line>> validate = lines -> {
        var lineNumber = new AtomicLong(1);
        return lines
                .map(addLineNumber.apply(lineNumber))
                .map(notStartWithBOMCheck)
                .map(startsEXTM3UCheck)
                .map(validLineCheck)
                .filter(not(emptyLine))
                .filter(not(commentLine))
                .map(validCharactersCheck)
                .map(normalizationCheck);
    };

}
