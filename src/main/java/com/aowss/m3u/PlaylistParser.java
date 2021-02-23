package com.aowss.m3u;

import java.nio.file.Path;
import java.util.stream.Stream;

public class PlaylistParser {

    static Playlist parse(Path path) {
        Stream<String> significantLines = PlaylistReader.streamFile(path);
        //significantLines.reduce()
        return null;
    }

}
