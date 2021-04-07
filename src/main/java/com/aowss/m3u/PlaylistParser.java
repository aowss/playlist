package com.aowss.m3u;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.aowss.m3u.Validator.validate;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class PlaylistParser {

    public static Function<Stream<String>, Playlist> parse = content -> {
        Stream<Line> lines = validate.apply(content);
        return new MediaPlaylist(lines.count());
    };

    public record Attribute(String name, String value) {
        public Attribute {
            if (name == null || name.isEmpty())
                throw new RuntimeException("The attribute name is mandatory");
            if (value == null || value.isBlank())
                throw new RuntimeException("The attribute value is mandatory");
            Scanner nameScanner = new Scanner(name);
            if (nameScanner.findInLine("[^A-Z0-9\\-]+]") != null)
                throw new RuntimeException("An attribute name can only contain the following characters: [A..Z], [0..9] and '-'");
        }
    }

    public static Function<String, Function<String, Map<String, String>>> attributesParser = separator -> content ->
        Arrays.stream(content.split(separator))
            .map(pair -> {
                String[] parts = pair.split("=");
                if (parts == null || parts.length != 2) throw new RuntimeException("An attribute should have a name and a value separated by '='");
                return new Attribute(parts[0], parts[1]);
            })
            .collect(toUnmodifiableMap(Attribute::name, Attribute::value));

}
