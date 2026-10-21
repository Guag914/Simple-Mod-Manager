package net.guag.simplemodmanager.modguard.general;

public enum ProjectType {
    MOD, RESOURCEPACK, SHADERPACK, MODPACK;

    //modrinth sends lowercase names, and "shader" rather than "shaderpack"
    public static ProjectType fromApi(String s) {
        if (s == null) return null;
        return switch (s) {
            case "mod" -> MOD;
            case "resourcepack" -> RESOURCEPACK;
            case "shader" -> SHADERPACK;
            case "modpack" -> MODPACK;
            default -> null;
        };
    }
}
