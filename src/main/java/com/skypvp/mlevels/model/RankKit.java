package com.skypvp.mlevels.model;

public class RankKit {

    private final String id;
    private final String permission;
    private final String command;

    public RankKit(String id, String permission, String command) {
        this.id = id;
        this.permission = permission;
        this.command = command;
    }

    public String getId() { return id; }
    public String getPermission() { return permission; }
    public String getCommand() { return command; }
}
