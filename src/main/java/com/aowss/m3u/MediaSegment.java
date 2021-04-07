package com.aowss.m3u;

import java.net.URI;
import java.util.Map;

public record MediaSegment(long duration, Map<String, String> tag, String title, URI uri) {
}
