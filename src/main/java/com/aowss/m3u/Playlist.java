package com.aowss.m3u;

public sealed interface Playlist permits MediaPlaylist, MasterPlaylist {
    long length();
}

record MediaPlaylist(long length) implements Playlist {
}

record MasterPlaylist(long length) implements Playlist {
}
