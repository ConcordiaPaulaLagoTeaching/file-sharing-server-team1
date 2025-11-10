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
        // Validate filename - check for null or empty
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Filename cannot be empty.");
        }

        // Validate filename length (max 11 characters)
        if (fileName.length() > 11) {
            throw new IllegalArgumentException("ERROR: Filename cannot exceed 11 characters.");
        }

        // Validate filename - no special characters (only alphanumeric, dots,
        // underscores, hyphens)
        if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException(
                    "ERROR: Filename contains invalid characters. Only alphanumeric, dots, underscores, and hyphens are allowed.");
        }

        globalLock.lock();
        try {
            // Check if file already exists in inode table
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i].isInUse() && inodeTable[i].getFilename().equals(fileName)) {
                    throw new IllegalArgumentException("ERROR: File '" + fileName + "' already exists.");
                }
            }

            // Find a free inode slot
            int freeSlot = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (!inodeTable[i].isInUse()) {
                    freeSlot = i;
                    break;
                }
            }

            // Check if maximum file limit reached
            if (freeSlot == -1) {
                throw new IllegalStateException("ERROR: Maximum file limit reached (" + MAXFILES + " files).");
            }

            // Create new FEntry with empty content (size = 0, firstBlock = -1)
            FEntry newEntry = new FEntry(fileName, (short) 0, (short) -1);

            // Update inode table with the new entry
            inodeTable[freeSlot] = newEntry;

            System.out.println("File '" + fileName + "' created successfully in inode slot " + freeSlot);

        } finally {
            globalLock.unlock();
        }
    }

    // TODO: Add readFile, writeFile and other required methods,
}
