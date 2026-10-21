package net.guag.simplemodmanager.modguard.general.buttons;

import net.guag.simplemodmanager.modguard.general.ProjectType;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URL;

public class DetailedCustomProjectWidget extends CustomProjectWidget {
    public DetailedCustomProjectWidget(int x, int y, Component message, OnPress onPress, URL imageUrl, String projectId, ProjectType type, String title, String author, String description, String categories, String lastModified, int downloads, int followers) throws IOException {
        super(x, y, message, onPress, imageUrl, projectId, type, title, author, description, categories, lastModified, downloads, followers);
    }

    //this class will contain extra info for each project but no render method, as that will be held by the screen
    //instead individual render methods will be used here, (e.g. render description, top bar etc.), and the screen will compile to render entire class
}