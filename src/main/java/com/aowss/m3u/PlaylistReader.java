package com.aowss.m3u;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.aowss.m3u.PlaylistParser.parse;
import static java.net.http.HttpClient.Version.HTTP_2;

public class PlaylistReader {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static Function<Path, Playlist> fromFile = filePath -> {
        if (!filePath.toString().endsWith(".m3u8") && !filePath.toString().endsWith(".m3u")) throw new RuntimeException("The path must end with either .m3u8 or .m3u");
        try {
            return parse.apply(Files.lines(filePath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error while reading the file located at " + filePath, e);
        }
    };

    public static Function<URI, Playlist> fromURI = uri -> {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();

        try {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            Predicate<String> isM3U = value -> value.equals("application/vnd.apple.mpegurl") || value.equals("audio/mpegurl");
            if ( !uri.getPath().endsWith(".m3u8") && !uri.getPath().endsWith(".m3u") && !response.headers().allValues("Content-Type").stream().anyMatch(isM3U) ) throw new RuntimeException("The URI must end with either .m3u8 or .m3u or the Content-Type must be either 'application/vnd.apple.mpegurl' or 'audio/mpegurl'");
            return parse.apply(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error while downloading the file from " + uri, e);
        }
    };

}
