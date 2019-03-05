package io.github.mosadie.plex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PlexApi {

    private final String PRODUCT_NAME;
    private final String PRODUCT_VERSION;
    private final String CLIENT_ID;
    private final String USERNAME;
    private final String AUTH_TOKEN;

    private final ResponseHandler<Document> responseHandler = new ResponseHandler<Document>() {

        public Document handleResponse(final HttpResponse response) throws IOException {
            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                if (entity != null) {
                    entity.writeTo(System.out);
                    System.out.println();
                }
                throw new HttpResponseException(
                        statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            if (entity == null) {
                throw new ClientProtocolException("Response contains no content");
            }
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                // ContentType contentType = ContentType.getOrDefault(entity);
                // if (!contentType.equals(ContentType.APPLICATION_XML)) {
                //     throw new ClientProtocolException("Unexpected content type:" +
                //         contentType);
                // }
                return docBuilder.parse(entity.getContent());
            } catch (ParserConfigurationException ex) {
                throw new IllegalStateException(ex);
            } catch (SAXException ex) {
                throw new ClientProtocolException("Malformed XML document", ex);
            }
        }
    
        };
    
    public PlexApi(String username, String password) throws IOException, AuthenticationException {
        this(username, password, "PlexAPI-Java");
    }
    
    public PlexApi(String username, String password, String productName) throws IOException, AuthenticationException {
        this(username, password, productName, "0.0");
    }
    
    public PlexApi(String username, String password, String productName, String productVersion) throws IOException, AuthenticationException {
        this(username, password, productName, productVersion, "0");
    }
    
    public PlexApi(String username, String password, String productName, String productVersion, String clientID) throws IOException, AuthenticationException {
        USERNAME = username;
        PRODUCT_NAME = productName;
        PRODUCT_VERSION = productVersion;
        CLIENT_ID = clientID;

        Document document = Request.Post("https://plex.tv/users/sign_in.xml")
            .addHeader("X-Plex-Product", PRODUCT_NAME)
            .addHeader("X-Plex-Version", PRODUCT_VERSION)
            .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
            .bodyString("user[login]=" + USERNAME + "&user[password]=" + password, ContentType.APPLICATION_FORM_URLENCODED)//.bodyForm(Form.form().add("user[login]", USERNAME).add("user[password]", password).build())
            .execute().handleResponse(responseHandler);

        if (document.getDocumentElement().getNodeName() != "user") {
            throw new AuthenticationException(); //TODO Find a better exeception to throw.
        }

        AUTH_TOKEN = document.getDocumentElement().getAttribute("authToken");
    }

    public List<PlexServer> getServers() {
        try {
            Document document = Request.Get("https://plex.tv/pms/servers.xml")
                .addHeader("X-Plex-Product", PRODUCT_NAME)
                .addHeader("X-Plex-Version", PRODUCT_VERSION)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .addHeader("X-Plex-Token", AUTH_TOKEN)
                .execute().handleResponse(responseHandler);

            if (document.getDocumentElement().getTagName() != "MediaContainer") {
                System.out.println("An error occured trying to get servers.");
                System.out.println(document.getTextContent()); //TODO check this.
                return null;
            }

            List<PlexServer> serverList = new ArrayList<>();

            try {
                Document localDocument = Request.Get("http://localhost:32400/")
                    .addHeader("X-Plex-Token", AUTH_TOKEN)
                    .execute().handleResponse(responseHandler);
                PlexServer server = new PlexServer(this, "127.0.0.1", "32400", "0", "local");
                serverList.add(server);
            } catch (Exception e) {
                // Ignore, likely means no local server on this machine.
            }

            NodeList elementList = document.getDocumentElement().getElementsByTagName("Server");
            for (int i = 0; i < elementList.getLength(); i++) {
                Element element = (Element)elementList.item(i);
                PlexServer server = new PlexServer(this, element.getAttribute("address"), element.getAttribute("port"), element.getAttribute("updatedAt"), element.getAttribute("machineIdentifier"));
                serverList.add(server);
            }
            return serverList;
        } catch (Exception e) {
            System.out.println("Exception getting server list: " + e.toString());
            e.printStackTrace(System.out);
            return null;
        }
    }

    public Map<String, String> getHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("X-Plex-Product", PRODUCT_NAME);
        map.put("X-Plex-Version", PRODUCT_VERSION);
        map.put("X-Plex-Client-Identifier", CLIENT_ID);
        map.put("X-Plex-Token", AUTH_TOKEN);
        return map;
    }

    public ResponseHandler<Document> getResponseHandler() {
        return responseHandler;
    }
}