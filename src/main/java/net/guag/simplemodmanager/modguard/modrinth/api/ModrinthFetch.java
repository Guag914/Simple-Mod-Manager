package net.guag.simplemodmanager.modguard.modrinth.api;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class ModrinthFetch {

    public String fetchSearchResults(ModrinthRequest request) throws IOException {

        URL url = request.buildURL();

        HttpsURLConnection connection =
                (HttpsURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "guag/simplemodmanager");

        int code = connection.getResponseCode();

        if (code != HttpsURLConnection.HTTP_OK) {

            throw new IOException(
                    "Modrinth returned HTTP " + code
            );

        }

        StringBuilder builder = new StringBuilder();

        try (Scanner scanner = new Scanner(connection.getInputStream())) {

            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }

        }

        return builder.toString();

    }

}