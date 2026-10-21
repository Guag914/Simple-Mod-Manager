package net.guag.simplemodmanager.modguard.modrinth.api;

import net.guag.simplemodmanager.modguard.general.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModrinthRequest {

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    private final Map<String, String> facets = new LinkedHashMap<>();

    private String query = "";
    private int offset = 0;
    private int limit = 20;
    private ProjectType type;

    public ModrinthRequest(ProjectType type) {
        this.type = type;
    }

    public void putFacet(String name, String value) {
        facets.put(name, value);
    }

    public void removeFacet(String name) {
        facets.remove(name);
    }

    public void clearFacets() {
        facets.clear();
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String tostring(){
        return "\nFacets: " + facets.toString() + "\n\nQuery: " + query + " \nLimit: " + limit + " Offset: " + offset;
    }

    public URL buildURL() throws MalformedURLException {

        StringBuilder jsonFacets = new StringBuilder("[");

        boolean first = true;

        for (Map.Entry<String, String> entry : facets.entrySet()) {

            if (!first) {
                jsonFacets.append(",");
            }

            jsonFacets.append("[\"")
                    .append(entry.getKey())
                    .append(":")
                    .append(entry.getValue())
                    .append("\"]");

            first = false;
        }

        // always include these

        if (!first) {
            jsonFacets.append(",");
        }

        if (type == ProjectType.MOD) {
            jsonFacets.append("[\"categories:fabric\"],");
            jsonFacets.append("[\"client_side:required\",\"client_side:optional\"],");
            jsonFacets.append("[\"project_type:mod\"]");
        } else if (type == ProjectType.RESOURCEPACK) {
            jsonFacets.append("[\"project_type:resourcepack\"]");
        } else if (type == ProjectType.SHADERPACK) {
            jsonFacets.append("[\"project_type:shader\"]");
        } else {
            LOGGER.error("Project type not supported yet: {}", type);
            throw new MalformedURLException("Project type not supported yet: " + type);
        }

        jsonFacets.append("]");

        String encodedFacets =
                URLEncoder.encode(jsonFacets.toString(), StandardCharsets.UTF_8);

        StringBuilder url =
                new StringBuilder("https://api.modrinth.com/v2/search?");

        if (!query.isBlank()) {

            url.append("query=")
                    .append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                    .append("&");

        }

        url.append("facets=")
                .append(encodedFacets)
                .append("&offset=")
                .append(offset)
                .append("&limit=")
                .append(limit);

        return new URL(url.toString());

    }
}