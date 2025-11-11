package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {
    private static final int FENTRY_SIZE = 15;
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final static FileSystemManager instance = null;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks
    private FNode[] fNodeTable;
    private int metadataBlocks;

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

    public void writeFile(String fileName, String content) throws Exception {
        // Validate filename
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Filename cannot be empty.");
        }

        // Validate content
        if (content == null) {
            throw new IllegalArgumentException("ERROR: Content cannot be null.");
        }

        globalLock.lock();
        try {
            // Find the file's inode in the inode table
            int inodeIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i].isInUse() && inodeTable[i].getFilename().equals(fileName)) {
                    inodeIndex = i;
                    break;
                }
            }

            // Check if file exists
            if (inodeIndex == -1) {
                throw new IllegalArgumentException("ERROR: File '" + fileName + "' not found.");
            }

            FEntry fileEntry = inodeTable[inodeIndex];
            byte[] contentBytes = content.getBytes();
            int contentSize = contentBytes.length;

            // Calculate required number of blocks for the content
            int requiredBlocks = (int) Math.ceil((double) contentSize / BLOCK_SIZE);

            // Check if we have enough free blocks
            int availableBlocks = 0;
            for (int i = 0; i < MAXBLOCKS; i++) {
                if (freeBlockList[i]) {
                    availableBlocks++;
                }
            }

            if (requiredBlocks > availableBlocks) {
                throw new IllegalStateException(
                        "ERROR: Insufficient disk space. Required: " + requiredBlocks + " blocks, Available: "
                                + availableBlocks + " blocks.");
            }

            // Allocate free blocks and write content
            int[] allocatedBlocks = new int[requiredBlocks];
            int blockCount = 0;

            // Find and allocate free blocks
            for (int i = 0; i < MAXBLOCKS && blockCount < requiredBlocks; i++) {
                if (freeBlockList[i]) {
                    allocatedBlocks[blockCount] = i;
                    freeBlockList[i] = false; // Mark block as used
                    blockCount++;
                }
            }

            // Write content to allocated blocks on disk
            for (int i = 0; i < requiredBlocks; i++) {
                int blockIndex = allocatedBlocks[i];
                long blockOffset = (long) blockIndex * BLOCK_SIZE;

                // Calculate how much content to write to this block
                int startOffset = i * BLOCK_SIZE;
                int bytesToWrite = Math.min(BLOCK_SIZE, contentSize - startOffset);

                // Write content to disk at the calculated offset
                disk.seek(blockOffset);
                disk.write(contentBytes, startOffset, bytesToWrite);
            }

            // Update the inode with block pointers and file size
            fileEntry.setFirstBlock((short) allocatedBlocks[0]);
            fileEntry.setFilesize((short) contentSize);

            System.out.println("File '" + fileName + "' written successfully with " + contentSize + " bytes across "
                    + requiredBlocks + " block(s).");

        } finally {
            globalLock.unlock();
        }
    }
    //This is the function to list the files iinside the file system
    public String[] listFiles() {
        globalLock.lock();
        try {
            List<String> fileList = new ArrayList<>();
            for (FEntry entry : inodeTable) {
                if (entry != null) {
                    fileList.add(entry.getFilename());
                }
            }
            return fileList.toArray(new String[0]);
        } finally {
            globalLock.unlock();
        }
    }
    //This block will be used when we need to reset data in tables to null
    private void overwriteBlocktoNull(int blockIndex) throws IOException {
        long position = metadataBlocks * BLOCK_SIZE + (blockIndex - metadataBlocks) * BLOCK_SIZE;
        disk.seek(position);
        disk.write(new byte[BLOCK_SIZE]);
        disk.getFD().sync();
    }
    private void writeMetadataToDisk() throws IOException {
        disk.seek(0);

        for (FEntry entry : inodeTable) {
            if (entry != null) {
                writeFEntry(entry);
            } else {
                disk.write(new byte[FENTRY_SIZE]);
            }
        }

        for (FNode node : fNodeTable) {
            disk.writeInt(node.getBlockIndex());
            disk.writeInt(node.getNext());
        }

        disk.getFD().sync();
    }
    //this will write the file entries to the file system
    private void writeFEntry(FEntry entry) throws IOException {
        byte[] filenameBytes = entry.getFilename().getBytes();
        disk.write(filenameBytes);
        for (int i = filenameBytes.length; i < 11; i++) {
            disk.writeByte(0);
        }
        disk.writeShort(entry.getFilesize());
        disk.writeShort(entry.getFirstBlock());
    }
    //this function will then delete the desired files from our system after we give it the file name
    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry targetEntry = null;
            int filesystemEntryIndex = -1;

            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) {
                    targetEntry = inodeTable[i];
                    filesystemEntryIndex = i;
                    break;
                }
            }

            if (targetEntry == null) {
                throw new Exception("ERROR: file '" + fileName + "' does'nt exist");
            }

            short currentBlock = targetEntry.getFirstBlock();
            while (currentBlock != -1) {
                FNode currentNode = fNodeTable[currentBlock];
                freeBlockList[currentBlock] = false;
                overwriteBlocktoNull(currentBlock);
                int nextBlock = currentNode.getNext();
                fNodeTable[currentBlock] = new FNode(-1);
                currentBlock = (short) nextBlock;
            }

            inodeTable[filesystemEntryIndex] = null;

            writeMetadataToDisk();
        } finally {
            globalLock.unlock();
        }
    }
    //this function will read the file from the saved data table list
    public byte[] readFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry targetEntry = null;
            for (FEntry entry : inodeTable) {
                if (entry != null && entry.getFilename().equals(fileName)) {
                    targetEntry = entry;
                    break;
                }
            }

            if (targetEntry == null) {
                throw new Exception("ERROR: this file :'" + fileName + "' does'nt exist");
            }

            byte[] content = new byte[targetEntry.getFilesize()];
            short currentBlock = targetEntry.getFirstBlock();
            int readBytes = 0;
            int blockIndex = 0;
            while (currentBlock != -1 && readBytes < targetEntry.getFilesize()) {
                long position = metadataBlocks * BLOCK_SIZE + (currentBlock - metadataBlocks) * BLOCK_SIZE;
                disk.seek(position);

                int bytesToRead = Math.min(BLOCK_SIZE, targetEntry.getFilesize() - readBytes);
                disk.readFully(content, readBytes, bytesToRead);

                readBytes += bytesToRead;

                if (blockIndex < fNodeTable.length - 1) {
                    currentBlock = (short) fNodeTable[currentBlock].getNext();
                } else {
                    currentBlock = -1;
                }
                blockIndex++;
            }

            return content;
        } finally {
            globalLock.unlock();
        }
    }

    // TODO: Add readFile, deleteFile and other required methods,
}
