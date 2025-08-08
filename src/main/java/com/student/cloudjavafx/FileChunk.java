package com.student.cloudjavafx;

import java.io.InputStream;

/**
 * Represents a single chunk of a file.
 * This class is used to manage file parts that are distributed across different servers.
 */
public class FileChunk {
    private int chunkId;
    private int fileId;
    private int chunkNumber;
    private int serverId;
    private String checksum;
    private InputStream data; // Represents the chunk's content

    /**
     * Constructor for creating a new chunk (before saving to DB).
     * This is the constructor that was missing and caused the error.
     * It accepts an InputStream for the chunk data.
     */
    public FileChunk(int fileId, int chunkNumber, int serverId, String checksum, InputStream data) {
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.serverId = serverId;
        this.checksum = checksum;
        this.data = data;
    }

    /**
     * Constructor for retrieving a chunk's metadata (from DB).
     * This constructor is used when you don't have the chunk's data yet.
     */
    public FileChunk(int chunkId, int fileId, int chunkNumber, int serverId, String checksum) {
        this.chunkId = chunkId;
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.serverId = serverId;
        this.checksum = checksum;
    }

    // Getters
    public int getChunkId() {
        return chunkId;
    }

    public int getFileId() {
        return fileId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public int getServerId() {
        return serverId;
    }

    public String getChecksum() {
        return checksum;
    }

    public InputStream getData() {
        return data;
    }

    // Setters (if needed, e.g., to set data after retrieval)
    public void setData(InputStream data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "FileChunk{" +
                "chunkId=" + chunkId +
                ", fileId=" + fileId +
                ", chunkNumber=" + chunkNumber +
                ", serverId=" + serverId +
                ", checksum='" + checksum + '\'' +
                '}';
    }
}
