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
    private String authToken;
    
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
        
        public PlexApi() throws IOException, AuthenticationException {
            this("PlexAPI-Java", "0.0", "0.0");
        }
        
        public PlexApi(String productName, String productVersion, String clientID) {
            PRODUCT_NAME = productName;
            PRODUCT_VERSION = productVersion;
            CLIENT_ID = clientID;
        }
        
        public boolean authenticate(String username, String password) {
            try {
                Document document = Request.Post("https://plex.tv/users/sign_in.xml")
                .addHeader("X-Plex-Product", PRODUCT_NAME)
                .addHeader("X-Plex-Version", PRODUCT_VERSION)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .bodyString("user[login]=" + username + "&user[password]=" + password, ContentType.APPLICATION_FORM_URLENCODED)//.bodyForm(Form.form().add("user[login]", USERNAME).add("user[password]", password).build())
                .execute().handleResponse(responseHandler);
                
                if (document.getDocumentElement().getNodeName() != "user") {
                    System.out.println("ERROR: Failed auth to Plex.tv. Response: " + document.getTextContent());
                    return false;
                }
                
                authToken = document.getDocumentElement().getAttribute("authToken");
                return true;
            } catch (IOException e) {
                System.out.println("ERROR: Failed auth to Plex.tv. Exception: " + e.toString());
                return false;
            }
        }

        public boolean authenticate(String authToken) {
            if (testAuth(authToken)) {
                this.authToken = authToken;
                return true;
            }
            return false;
        }

        public String startPinAuth() {
            try {
                Document document = Request.Post("https://plex.tv/pins.xml")
                .addHeader("X-Plex-Product", PRODUCT_NAME)
                .addHeader("X-Plex-Version", PRODUCT_VERSION)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .execute().handleResponse(getResponseHandler());

                if (!document.getDocumentElement().getNodeName().equals("pin")) {
                    System.out.println("ERROR: Something went wrong getting a id for pin auth.");
                    return null;
                }

                String code = document.getDocumentElement().getElementsByTagName("code").item(0).getTextContent();
                String id = document.getDocumentElement().getElementsByTagName("id").item(0).getTextContent();

                PlexPinAuthThread pinAuthThread = new PlexPinAuthThread(id, this);
                pinAuthThread.start();
                return code;
            } catch (Exception e) {
                return "ERROR";
            }
        }
        
        public List<PlexServer> getServers() {
            if (authToken == null) {
                return null;
            }
            try {
                Document document = Request.Get("https://plex.tv/pms/servers.xml")
                .addHeader("X-Plex-Product", PRODUCT_NAME)
                .addHeader("X-Plex-Version", PRODUCT_VERSION)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .addHeader("X-Plex-Token", authToken)
                .execute().handleResponse(responseHandler);
                
                if (document.getDocumentElement().getTagName() != "MediaContainer") {
                    System.out.println("An error occured trying to get servers.");
                    System.out.println(document.getTextContent()); //TODO check this.
                    return null;
                }
                
                List<PlexServer> serverList = new ArrayList<>();
                
                NodeList elementList = document.getDocumentElement().getElementsByTagName("Server");
                for (int i = 0; i < elementList.getLength(); i++) {
                    Element element = (Element)elementList.item(i);
                    PlexServer server = new PlexServer(this, element.getAttribute("address"), element.getAttribute("port"), element.getAttribute("updatedAt"), element.getAttribute("machineIdentifier"));
                    PlexServer localServer = new PlexServer(this, element.getAttribute("localAddresses"), "32400", element.getAttribute("updatedAt"), "local-" + element.getAttribute("machineIdentifier"));
                    if (localServer.canConnect()) {
                        serverList.add(localServer);
                    } else if (server.canConnect()) {
                        serverList.add(server);
                    }
                }
                return serverList;
            } catch (Exception e) {
                System.out.println("Exception getting server list: " + e.toString());
                e.printStackTrace(System.out);
                return null;
            }
        }

        private boolean testAuth(String authToken) {
            try {
                Document document = Request.Get("https://plex.tv/pms/servers.xml")
                .addHeader("X-Plex-Product", PRODUCT_NAME)
                .addHeader("X-Plex-Version", PRODUCT_VERSION)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .addHeader("X-Plex-Token", authToken)
                .execute().handleResponse(responseHandler);
                
                if (document.getDocumentElement().getTagName() != "MediaContainer") {
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        public Map<String, String> getHeaders() {
            Map<String, String> map = new HashMap<>();
            map.put("X-Plex-Product", PRODUCT_NAME);
            map.put("X-Plex-Version", PRODUCT_VERSION);
            map.put("X-Plex-Client-Identifier", CLIENT_ID);
            if (authToken != null) map.put("X-Plex-Token", authToken);
            return map;
        }
        
        public ResponseHandler<Document> getResponseHandler() {
            return responseHandler;
        }

        public boolean isAuthenticated() {
            return authToken != null;
        }

        public String getAuthToken() {
            return authToken;
        }
    }