package io.github.mosadie.plex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PlexServer {
    private final PlexApi plex;
    private final String address;
    private final String port;
    private final String updatedAt;
    private final String uniqueId;

    public PlexServer(PlexApi plex, String address, String port, String updatedAt, String uniqueId) {
        this.plex = plex;
        this.address = address;
        this.port = port;
        this.updatedAt = updatedAt;
        this.uniqueId = uniqueId;
    }

    public String toString() {
        return "PlexServer:" + address + ":" + port + " Updated " + updatedAt;
    }

    public String getUrl() {
        return getUrl("", false);
    }

    public String getUrl(String suffix) {
        return getUrl(suffix, false);
    }

    public String getUrl(String suffix, boolean includeToken) {
        return "http://" + address + ":" + port + suffix +
            (includeToken ? "?X-Plex-Token=" + plex.getHeaders().get("X-Plex-Token") : "");
    }

    public List<PlexLibrary> getLibraries() {
        Map<String, String> headers = plex.getHeaders();
        Request request = Request.Get(getUrl("/library/sections"));
        Document document;
        for(String key : headers.keySet()) {
            request.addHeader(key, headers.get(key));
        }
        try {
            document = request.execute().handleResponse(plex.getResponseHandler());
        } catch (Exception e) {
            System.out.println("Something went wrong getting libraries of server " + toString());
            System.out.println("Exception: " + e.toString());
            e.printStackTrace(System.out);
            return null;
        }
        
        if (document.getDocumentElement().getTagName() != "MediaContainer") {
            System.out.println("Something went wrong getting libraries of server " + toString());
            return null;
        }

        List<PlexLibrary> libraries = new ArrayList<>();
        NodeList list = document.getElementsByTagName("Directory");
        for(int i = 0; i < list.getLength(); i++) {
            Element element = (Element)list.item(i);
            libraries.add(new PlexLibrary(this, element.getAttribute("title"), element.getAttribute("key"), element.getAttribute("type")));
        }
        return libraries;
    }

    public List<PlexMusicTrack> searchTracks(String query) {
        Map<String, String> headers = plex.getHeaders();
        Request request = Request.Get(getUrl("/search?type=10&query=" + query));
        Document document;
        for(String key : headers.keySet()) {
            request.addHeader(key, headers.get(key));
        }
        try {
            document = request.execute().handleResponse(plex.getResponseHandler());
        } catch (Exception e) {
            System.out.println("Something went wrong searching tracks on " + toString());
            System.out.println("Exception: " + e.toString());
            e.printStackTrace(System.out);
            return null;
        }

        NodeList nodeList = document.getElementsByTagName("Track");
        List<PlexMusicTrack> tracks = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element)nodeList.item(i);
            String url = ((Element)element.getFirstChild().getFirstChild()).getAttribute("key");
            PlexMusicTrack track = new PlexMusicTrack(this, element.getAttribute("title"), element.getAttribute("originalTitle"), url);
            tracks.add(track);
        }

        return tracks;
    }
}