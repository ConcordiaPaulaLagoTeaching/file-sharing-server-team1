package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final static FileSystemManager instance = null;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if (instance == null) {
            try {
                // Initialize the RandomAccessFile (disk)
                this.disk = new RandomAccessFile(filename, "rw");
                this.disk.setLength(totalSize);

                // Initialize the inode table with MAXFILES entries
                this.inodeTable = new FEntry[MAXFILES];
                for (int i = 0; i < MAXFILES; i++) {
                    this.inodeTable[i] = new FEntry();
                }

                // Initialize the free block list (bitmap) with MAXBLOCKS entries
                this.freeBlockList = new boolean[MAXBLOCKS];
                for (int i = 0; i < MAXBLOCKS; i++) {
                    this.freeBlockList[i] = true; // All blocks initially free
                }

                // Calculate metadata size and mark initial blocks as used
                int metadataSize = (15 * MAXFILES) + (8 * MAXBLOCKS);
                int metadataBlocks = (int) Math.ceil((double) metadataSize / BLOCK_SIZE);

                // Mark metadata blocks as used
                for (int i = 0; i < metadataBlocks && i < MAXBLOCKS; i++) {
                    this.freeBlockList[i] = false;
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize file system: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    // TODO: Add readFile, writeFile and other required methods,
}
