package net.guag.simplemodmanager.modguard.modrinth.testing;

import net.guag.simplemodmanager.modguard.general.buttons.CustomProjectWidget;
import net.guag.simplemodmanager.modguard.general.services.Parser;
import net.guag.simplemodmanager.modguard.modrinth.api.ModrinthFetch;
import net.guag.simplemodmanager.modguard.modrinth.api.ModrinthRequest;
import net.guag.simplemodmanager.modguard.general.ProjectType;

import java.io.IOException;
import java.util.ArrayList;

public class ManualModrinthTesting {

    public static void run() throws IOException {
        main();
    }

    public static void main(String... args) throws IOException { //use ... to avoid parameter when not necessary
        ModrinthRequest request = new ModrinthRequest(ProjectType.MOD); //create request and only specify type ONCE
        ModrinthFetch fetch = new ModrinthFetch();

        //add facets
        request.setQuery("simple mod manager");
        request.putFacet("versions", "26.2");

        String json = fetch.fetchSearchResults(request); //fetch as json
        System.out.println(json); //print for testing

        //parse the json into an array of project widgets
        ArrayList<CustomProjectWidget> widgets = Parser.parse(json);

        if (widgets == null) { System.out.println("Parse failed, check the log"); return; }
        //print for testing
        for (int i = 0; i < widgets.size(); i++){
            System.out.println(widgets.get(i).toString());
        }
    }
}
