package com.aowss.m3u;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.aowss.m3u.Validator.validate;

public class PlaylistParser {

    public static Function<Stream<String>, Playlist> parse = content -> {
        Stream<Line> lines = validate.apply(content);
        return new Playlist(lines.count());
    };

}
