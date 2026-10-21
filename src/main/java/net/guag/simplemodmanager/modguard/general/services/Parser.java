package net.guag.simplemodmanager.modguard.general.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.guag.simplemodmanager.modguard.general.ProjectType;
import net.guag.simplemodmanager.modguard.general.buttons.CustomProjectWidget;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class Parser {

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    public static ArrayList<CustomProjectWidget> parse(String json) {
        try {
            JsonElement rootElement = JsonParser.parseString(json);
            if (!rootElement.isJsonObject()) return null;
            JsonObject rootObject = rootElement.getAsJsonObject();

            ArrayList<CustomProjectWidget> widgets = new ArrayList<>();

            if (rootObject.has("hits") && rootObject.get("hits").isJsonArray()) {
                for (JsonElement hitElement : rootObject.getAsJsonArray("hits")) {
                    if (!hitElement.isJsonObject()) continue;
                    JsonObject hit = hitElement.getAsJsonObject();

                    try { // one bad hit shouldn't kill the whole list
                        String iconURL = str(hit, "icon_url");
                        String projectId = str(hit, "project_id");
                        if (projectId == null) continue;

                        String projectTitle = str(hit, "title");
                        String author = str(hit, "author");
                        String description = str(hit, "description");
                        String lastModified = str(hit, "date_modified");
                        ProjectType projectType = ProjectType.fromApi(str(hit, "project_type"));

                        int downloads = hit.has("downloads") && !hit.get("downloads").isJsonNull() ? hit.get("downloads").getAsInt() : 0;
                        int followers = hit.has("follows") && !hit.get("follows").isJsonNull() ? hit.get("follows").getAsInt() : 0;

                        String categories = "";
                        if (hit.has("display_categories") && hit.get("display_categories").isJsonArray()) {
                            JsonArray arr = hit.getAsJsonArray("display_categories");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < arr.size(); i++) {
                                sb.append(arr.get(i).getAsString());
                                if (i < arr.size() - 1) sb.append(", ");
                            }
                            categories = sb.toString();
                        }

                        URL iconUrl = (iconURL == null || iconURL.isBlank()) ? null : URI.create(iconURL).toURL();

                        widgets.add(new CustomProjectWidget(20, widgets.size() * 80, Component.literal(""),
                                _ -> {},
                                iconUrl, projectId, projectType, projectTitle, author, description,
                                categories, lastModified, downloads, followers));
                    } catch (Exception e) {
                        LOGGER.warn("Skipping a hit that could not be parsed", e);
                    }
                }
                return widgets;
            }
        } catch (Exception e) { LOGGER.error("Critical: Could not parse JSON or create widgets: {}", json, e); }

        return null;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
