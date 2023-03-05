package com.husam.storageengine.core;

public class DatabaseConfig {

  private static DatabaseConfig instance;

  private int pageSize;

  private DatabaseConfig() {
    // Private constructor to prevent instantiation from outside
  }

  public static synchronized DatabaseConfig getInstance() {
    if (instance == null) {
      instance = new DatabaseConfig();
    }
    return instance;
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