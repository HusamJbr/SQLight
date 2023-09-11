package come.husam.cachemanager;

import com.husam.cachemanager.BufferPoolManager;
import com.husam.cachemanager.BufferPoolManagerImpl;
import com.husam.storageengine.diskmanager.DiskManager;
import com.husam.storageengine.page.Page;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BufferPoolManagerImplTest {
    private static final String DB_FILE_NAME = "test.db";
    private static final int BUFFER_POOL_SIZE = 10;
    private DiskManager diskManager;
    private BufferPoolManager bpm;
    @Before
    public void setUp() {
        this.diskManager = new DiskManager(DB_FILE_NAME);
        this.bpm = new BufferPoolManagerImpl(BUFFER_POOL_SIZE, this.diskManager, 0);
    }

    @Test
    public void testBufferPool() {
        Page page0 = bpm.newPage();

        // Scenario: The buffer pool is empty. We should be able to create a new page.
        assertNotNull(page0);
        assertEquals(0, page0.getPageId());

        // Scenario: Once we have a page, we should be able to read and write content.
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; i++) {
            page0.getData()[i] = data[i];
        }
        assertEquals("Hello", new String(Arrays.copyOfRange(page0.getData(), 0, 5), StandardCharsets.UTF_8));

        // Scenario: We should be able to create new pages until we fill up the buffer pool.
        for (int i = 1; i < BUFFER_POOL_SIZE; ++i) {
            assertNotNull(bpm.newPage());
        }

        // Scenario: Once the buffer pool is full, we should not be able to create any new pages.
        for (int i = BUFFER_POOL_SIZE; i < BUFFER_POOL_SIZE * 2; ++i) {
            assertNull(bpm.newPage());
        }

        // Scenario: After unpinning pages {0, 1, 2, 3, 4} and pinning another 4 new pages,
        // there would still be one buffer page left for reading page 0.
        for (int i = 0; i < 5; ++i) {
            assertTrue(bpm.unpinPage(i, true));
        }
        for (int i = 0; i < 4; ++i) {
            assertNotNull(bpm.newPage());
        }

        // Scenario: We should be able to fetch the data we wrote a while ago.
        page0 = bpm.fetchPage(0);
        assertNotNull(page0);
        assertEquals("Hello", new String(Arrays.copyOfRange(page0.getData(), 0, 5), StandardCharsets.UTF_8));

        // Scenario: If we unpin page 0 and then make a new page, all the buffer pages should
        // now be pinned. Fetching page 0 again should fail.
        assertTrue(bpm.unpinPage(0, true));
        assertNotNull(bpm.newPage());
        assertNull(bpm.fetchPage(0));
        this.diskManager.shutDown();
        File file = new File("test.db");
        file.delete();
    }
}
