package com.husam.common;

public class DatabaseConfig {

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    private int pageSize;

    private DatabaseConfig() {
        // Private constructor to prevent instantiation from outside
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public void load() {
        // Load config from file
    }

    public int getPageSize() {
        return pageSize;
    }

    public void save() {
        // Save config to file
    }

}