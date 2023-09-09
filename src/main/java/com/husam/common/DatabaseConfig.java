package com.husam.common;

import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConfig {

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    private int pageSize = 4096;
    private int invalidPageId = -1;
    private int invalidFrameId = -1;

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

    public int getInvalidPageId() {
        return invalidPageId;
    }

    public int getInvalidFrameId() {
        return invalidFrameId;
    }

    public void save() {
        // Save config to file
    }

}