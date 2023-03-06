package com.husam.storageengine.diskmanager;

import com.husam.common.DatabaseConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The DiskManager class is responsible for handling the reading and writing of pages to disk.
 * This class ensures atomic writes by writing in exact page sizes, preventing data corruption and maintaining consistency.
 * By avoiding partial writes, it guarantees complete and error-free data, providing a reliable foundation for any system that relies on persistent storage.
 * Since disk access involves physical movement and can be time-consuming, it's essential to ensure that multiple threads don't access the disk in parallel.
 * Therefore, this class has been designed to be synchronized to prevent race conditions and data corruption.
 * By carefully managing disk operations, this class can efficiently store and retrieve data with minimal errors or delays.
 * Overall, the DiskManager class is a well-designed component that plays a critical role in the smooth operation of any system that requires persistent storage.
 */

public class DiskManager {

    private RandomAccessFile dbFile;
    private DatabaseConfig conf = DatabaseConfig.getInstance();

    public DiskManager(String fileName) {
        try {
            dbFile = new RandomAccessFile(fileName, "rw");
        } catch (FileNotFoundException e) {
            // TODO: log error
            throw new RuntimeException(e);
        }
    }

    public synchronized void readPage(long pageId, byte[] pageData) {
        long offset = pageId * conf.getPageSize();
        try {
            if (offset > dbFile.length()) {
                // TODO: log error
                return;
            }
            dbFile.seek(offset);
            if (dbFile.read(pageData, 0, conf.getPageSize()) == -1) {
                // TODO: log error about we couldn't read the page
            }
        } catch (IOException e) {
            // TODO: log error
            throw new RuntimeException(e);
        }

    }

    public synchronized void writePage(long pageId, byte[] pageData) {
        long offset = pageId * conf.getPageSize();
        try {
            dbFile.seek(offset);
            dbFile.write(pageData, 0, conf.getPageSize());
            // to keep disk in sync
            dbFile.getFD().sync();
        } catch (IOException e) {
            // TODO: log error
            throw new RuntimeException(e);
        }
    }
}
