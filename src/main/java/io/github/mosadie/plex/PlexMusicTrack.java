package io.github.mosadie.plex;

public class PlexMusicTrack {
    private final String title;
    private final String artist;
    private final String partialUrl;
    private final PlexServer server;

    public PlexMusicTrack(PlexServer server, String title, String artist, String partialUrl) {
        this.server = server;
        this.title = title;
        this.artist = artist;
        this.partialUrl = partialUrl;
    }

    public String getMediaFileURL(boolean includeToken) {
        return server.getUrl(partialUrl, includeToken);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}