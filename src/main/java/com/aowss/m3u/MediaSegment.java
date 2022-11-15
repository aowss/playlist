package com.aowss.m3u;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public record MediaSegment(float duration, Map<String, String> attributes, String title, URI uri) {

    static MediaSegment parse(Line extinfTag, Line uri) {
        if (extinfTag == null || uri == null || extinfTag.content().isBlank() || uri.content().isBlank()) throw new RuntimeException("A media segment must have an EXTINF tag and a URI");
        String tag = extinfTag.content();
        if (!tag.startsWith("#EXTINF:")) throw new RuntimeException("A media segment must start with an EXTINF tag");
        int firstComma = tag.indexOf(',');
        int firstSpace = tag.indexOf(' ');
        boolean noAttributes = firstSpace == -1 || firstComma < firstSpace;
        int durationEnd = noAttributes ? firstComma : firstSpace;
        int titleStart = tag.lastIndexOf(',');
        float duration = Float.valueOf(tag.substring(8, durationEnd));
        String title = tag.substring(titleStart + 1).trim();
        URI link = URI.create(uri.content());
        Map<String, String> attributes = noAttributes ? Map.of() : parseAttributes(tag.substring(durationEnd, titleStart).trim());
        return new MediaSegment(duration, attributes, title, link);
    }

    static Map<String, String> parseAttributes(String attributesList)  {
        Map<String, String> attributes = new HashMap<>();
        int start = -1;
        String name = null;
        boolean quoted = false;
        for (int i = 0; i < attributesList.length(); i++) {
            switch (attributesList.charAt(i)) {
                case ' ' -> {
                    if (start != -1) {
                        if (name == null) {
                            name = attributesList.substring(start, i);
                            start = -1;
                        } else {
                            if (!quoted) {
                                attributes.put(name, attributesList.substring(start, i));
                                name = null;
                                start = -1;
                            }
                        }
                    }
                }
                case '=' -> {
                    if (name == null) {
                        name = attributesList.substring(start, i);
                        start = -1;
                    }
                }
                case '"' -> {
                    if (quoted) {
                        attributes.put(name, attributesList.substring(start, i));
                        name = null;
                        start = -1;
                        quoted = false;
                    } else {
                        quoted = true;
                    }
                }
                default -> {
                    if (start == -1) start = i;
                }
            }
        }
        if (name != null) {
            attributes.put(name, attributesList.substring(start));
        }
        return attributes;
    }
}
