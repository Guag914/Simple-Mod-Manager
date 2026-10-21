package net.guag.simplemodmanager.modguard.general.buttons;

import net.guag.simplemodmanager.modguard.general.ProjectType;
import net.guag.simplemodmanager.modguard.general.services.DownloadImageFromURL;
import net.guag.simplemodmanager.util.DrawingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;

public class CustomProjectWidget extends Button {

    public String projectTitle; //title
    public String projectId;
    public ProjectType projectType;
    public String author; //author
    public String description; //description
    public String categories; //display_categories
    public String lastModified; //date_modified

    public int downloads; //downloads
    public int followers; //follows

    //assigned after downloading
    public final Identifier logoID;

    public CustomProjectWidget(int x, int y, Component message, OnPress onPress,
        URL imageUrl, String projectId, ProjectType type, String title, String author, String description, String categories, String lastModified,
        int downloads, int followers
    ) throws IOException {

        super(x, y, 380, 72, message, onPress, DEFAULT_NARRATION);

        this.projectTitle = title;
        this.projectId = projectId;
        this.projectType = type;
        this.author = author;
        this.description = description;
        this.categories = categories;
        this.lastModified = lastModified;

        this.downloads = downloads;
        this.followers = followers;

        this.logoID = DownloadImageFromURL.download(projectId, imageUrl);
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        var font = Minecraft.getInstance().font;
        int x = getX(), y = getY();

        graphics.fill(x, y, x + 380, y + 72, 0xFF878B8F);
        if (DownloadImageFromURL.isLoaded(logoID)) DrawingUtils.renderImage(logoID, graphics, x + 8, y + 8, 32);

        graphics.text(font, nz(projectTitle), x + 48, y + 8, 0xFFFFFFFF, false);
        graphics.text(font, "by " + nz(author), x + 48, y + 16, 0xFFD0D0D0, false);
        graphics.text(font, nz(description), x + 8, y + 24, 0xFFD0D0D0, false);
        graphics.text(font, nz(categories), x + 8, y + 32, 0xFFD0D0D0, false);
        graphics.text(font, downloads + "  |  " + followers, x + 8, y + 40, 0xFFD0D0D0, false);
        graphics.text(font, nz(lastModified), x + 8, y + 48, 0xFFD0D0D0, false);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override
    public String toString() {
        return "CustomProjectWidget{" +
                "projectTitle='" + projectTitle + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", categories='" + categories + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", downloads=" + downloads +
                ", followers=" + followers +
                ", logoID=" + logoID +
                '}';
    }

}
