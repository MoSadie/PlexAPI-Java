package io.github.mosadie.plex;

public class PlexLibrary {
    public static enum LIBRARY_TYPE { MUSIC, PHOTO, MOVIE, TV, OTHER};

    private final LIBRARY_TYPE type;
    private final String name;
    private final String id;
    private final PlexServer server;

    public PlexLibrary(PlexServer server, String name, String id, String type) {
        this.server = server;
        this.name = name;
        this.id = id;
        switch(type) {
            case "artist":
                this.type = LIBRARY_TYPE.MUSIC;
                break;
            case "photo":
                this.type = LIBRARY_TYPE.PHOTO;
                break;
                //TODO: Determine the rest of the values
            default:
                this.type = LIBRARY_TYPE.OTHER;
        }
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public LIBRARY_TYPE getType() {
        return type;
    }
}