package io.github.mosadie.plex;

import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.w3c.dom.Document;

public class PlexPinAuthThread extends Thread {
    private final String id;
    private final PlexApi api;
    public PlexPinAuthThread(String id, PlexApi api) {
        this.id = id;
        this.api = api;
    }
    
    public void run() {
        try {
            boolean finished = false;
            while (!finished) {
                try {
                    Map<String, String> headers = api.getHeaders();
                    Document document = Request.Get("https://plex.tv/pins/" + id + ".xml")
                    .addHeader("X-Plex-Product", headers.get("X-Plex-Product"))
                    .addHeader("X-Plex-Version", headers.get("X-Plex-Version"))
                    .addHeader("X-Plex-Client-Identifier", headers.get("X-Plex-Client-Identifier"))
                    .execute().handleResponse(api.getResponseHandler());

                    boolean haveAuthToken = document.getElementsByTagName("auth_token").item(0).getTextContent() != null &&
                                            document.getElementsByTagName("auth_token").item(0).getTextContent() != "";

                    if (haveAuthToken) {
                        finished = true;
                        api.authenticate(document.getElementsByTagName("auth_token").item(0).getTextContent());
                    }
                    
                } catch (Exception e) {
                    //TODO something?
                    finished = true;
                }
                sleep(1000);
            }
        } catch (InterruptedException e) {

        }
    }
}