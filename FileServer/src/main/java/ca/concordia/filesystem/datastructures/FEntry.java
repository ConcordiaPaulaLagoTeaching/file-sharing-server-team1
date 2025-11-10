package ca.concordia.filesystem.datastructures;

public class FEntry {

    private String filename; // Max 11 characters
    private short filesize;
    private short firstBlock; // -1 if no blocks allocated

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    // Default constructor creates an unused entry
    public FEntry() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(short firstBlock) {
        this.firstBlock = firstBlock;
    }

    public boolean isInUse() {
        return filename != null && !filename.isEmpty();
    }

    public boolean hasBlocks() {
        return firstBlock != -1;
    }

    public void clear() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }

    @Override
    public String toString() {
        return String.format("FEntry{filename='%s', filesize=%d, firstBlock=%d, inUse=%b}",
                filename, filesize, firstBlock, isInUse());
    }
}
