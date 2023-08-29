package com.husam.common;

public class DatabaseConfig {

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    private int PAGE_SIZE = 4096;
    private int INVALID_PAGE_ID = -1;

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
        return PAGE_SIZE;
    }

    public int getInvalidPageId() {
        return INVALID_PAGE_ID;
    }

    public void save() {
        // Save config to file
    }

}