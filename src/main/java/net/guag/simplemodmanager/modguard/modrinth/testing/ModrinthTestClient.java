package net.guag.simplemodmanager.modguard.modrinth.testing;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

///NOTE: ThIS CLASS IS FOR TESTING ONLY, IT RETURNS A STRING OF THE API OUTPUT IN CONSOLE AND NOTHING ELSE
public class ModrinthTestClient {
    public ModrinthTestClient() throws IOException {}

    private URL baseURL = new URL("https://api.modrinth.com/v2/search");
    private HashMap<String, String> facets = new HashMap<>();;
    private String getterType; //"version" (for specific download), "project" (for listing), "projectDetails" (for specifics)
    private String query;

    //Used for page data
    public Integer offset;
    public Integer limit;

    private HttpsURLConnection connection;

    private String finalJSONToText;

    public String initRequest(String type) throws IOException {
        Scanner sc = new Scanner (System.in);

        if (Objects.equals(type, "project")){

            String facetName = "";
            String facetContent;

            //Get user query/facets (expand to use minecraft inputs with text widget later)
            //System.out.println("Enter a search query: "); query = sc.nextLine().trim();;

//            while (true){
//                System.out.println("Enter a facet name: "); facetName = sc.nextLine().trim(); if (facetName.equals("break")) break; //See modrinth documentation
//                System.out.println("Enter " + facetName + "'s content: "); facetContent = sc.nextLine().trim();;
//                facets.put(facetName, facetContent);
//            }

            //Flags
            //System.out.println("Enter an offset: "); offset = sc.nextInt(); sc.nextLine();
            //System.out.println("Enter a limit: "); limit = sc.nextInt(); sc.nextLine(); //less than 100, default 10

            System.out.println(sendRequest(facets, query, offset, limit));

            return String.valueOf(sendRequest(facets, query, offset, limit));

            //EXAMPLE (w/ query): https://api.modrinth.com/v2/search?facets=[["project_type:mod"]]&limit=50
            //EXAMPLE (w/o query): https://api.modrinth.com/v2/search?query=optimization&limit=50&facets=[["project_type:mod"],["categories:fabric"],["versions:1.21.7"]]


        }

        return "";
    }

    public void putFacet(String facetName, String facetContent ){
        facets.put(facetName, facetContent);
    }

    public StringBuilder sendRequest(HashMap<String, String> facets, String search, int offset, int limit) throws IOException {
        //Build proper JSON format
        StringBuilder jsonFacets = new StringBuilder();
        jsonFacets.append("[");
        boolean first = true;
        for (String key : facets.keySet()) {
            if (!first) {
                jsonFacets.append(",");
            }
            jsonFacets.append("[\"").append(key).append(":").append(facets.get(key)).append("\"]");
            first = false;
        }

        if (facets.isEmpty()){ jsonFacets.append("[\"categories:fabric\"], [\"client_side:required\", \"client_side:optional\"]");}
        else { jsonFacets.append(", [\"categories:fabric\"], [\"client_side:required\", \"client_side:optional\"], [\"project_type:mod\"]"); }

        jsonFacets.append("]");
        System.out.println("Formatted Modrinth facets: \n" + jsonFacets.toString());

        String encodedFacets = URLEncoder.encode(jsonFacets.toString(), StandardCharsets.UTF_8); //Enocde strings to %XX codes for URL request
        // MAKE SURE TO ALWAYS APPEND TO "client_side":"required", "client_side":"optional" to avoid facet error 400 (blank facet-malformed URL)
        // ["name:content"],AND["name:content",OR"name:content"]

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        //Send request
        if (search.isBlank()){
            URL formattedURL = new URL("https://api.modrinth.com/v2/search?facets=" + encodedFacets
                    + "&offset=" + offset
                    + "&limit=" + limit
            );

            connection = (HttpsURLConnection) formattedURL.openConnection();
            connection.setRequestMethod("GET"); //Using GET request for all modrinth fetches

            //Handle Error Codes
            if (!retrieveResponseCode(connection).equals("OK")) {System.out.println(retrieveResponseCode(connection));}
            return formatJSON(connection);

        } else{
            URL formattedURL = new URL("https://api.modrinth.com/v2/search?query=" + encodedQuery
                    + "&facets=" + encodedFacets
                    + "&offset=" + offset
                    + "&limit=" + limit
            );

            connection = (HttpsURLConnection) formattedURL.openConnection();
            connection.setRequestMethod("GET"); //Using GET request for all modrinth fetches
            return formatJSON(connection);
        }
    }

    public StringBuilder formatJSON(HttpsURLConnection connection) throws IOException {
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(connection.getInputStream());
        while (scanner.hasNext()) {
            result.append(scanner.nextLine());
        }
        scanner.close();
        return result;
    }

    public String retrieveResponseCode(HttpsURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {/* 200 OK — proceed normally */ return "OK";}
        else {/* Any other response code — handle as error */ return "Request failed with HTTP code: " + responseCode;}

    }

    public static void main(String[] args) {

        try {
            ModrinthTestClient fetcher = new ModrinthTestClient();
            // Pass "project" to trigger your current initRequest logic
            fetcher.initRequest("project");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Getters and setters:
    public void setQuery(String q){
        this.query = q;
    }

    public void setOffset(Integer o){
        this.offset = o;
    }

    public void setLimit(Integer l){
        this.limit = l;
    }

}
