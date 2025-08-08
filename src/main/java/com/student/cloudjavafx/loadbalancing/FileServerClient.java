package com.student.cloudjavafx.loadbalancing;

import com.student.cloudjavafx.FileChunk;
import com.student.cloudjavafx.utils.LogManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates a client for interacting with a distributed file server.
 * In a real application, this would involve network communication (e.g., HTTP, gRPC).
 * For this simulation, it uses an in-memory map to represent server storage.
 */
public class FileServerClient {
    private final int serverId;
    // Simulates storage on a file server: Map<ServerId, Map<FileId, Map<ChunkNumber, ChunkData>>>
    // Note: The outer map key is serverId, as this class represents a client for a specific server.
    // The inner map is for fileId -> Map<ChunkNumber, ChunkData>
    private static final Map<Integer, Map<Integer, Map<Integer, byte[]>>> serverStorage = new ConcurrentHashMap<>();

    public FileServerClient(int serverId) {
        this.serverId = serverId;
        // Initialize storage for this specific server if not already present
        serverStorage.putIfAbsent(serverId, new ConcurrentHashMap<>());
    }

    /**
     * Stores a file chunk on this simulated file server.
     *
     * @param chunk The FileChunk object containing data and metadata.
     * @return true if successful, false otherwise.
     */
    public boolean storeChunk(FileChunk chunk) {
        try {
            // Simulate network delay
            Thread.sleep(100);

            Map<Integer, Map<Integer, byte[]>> fileChunks = serverStorage.get(serverId);
            fileChunks.putIfAbsent(chunk.getFileId(), new ConcurrentHashMap<>());
            // Read the data from the InputStream and store it as a byte array
            fileChunks.get(chunk.getFileId()).put(chunk.getChunkNumber(), chunk.getData().readAllBytes());

            System.out.println(String.format("FileServerClient (Server %d): Stored chunk %d for file %d.",
                                             serverId, chunk.getChunkNumber(), chunk.getFileId()));
            LogManager.logServerAction(serverId, "STORE_CHUNK",
                                       String.format("Chunk %d for file %d stored successfully.", chunk.getChunkNumber(), chunk.getFileId()));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("FileServerClient (Server " + serverId + "): Interrupted while storing chunk.");
            LogManager.logServerError(serverId, "STORE_CHUNK_FAILED", "Interrupted while storing chunk.");
            return false;
        } catch (java.io.IOException e) {
            System.err.println("FileServerClient (Server " + serverId + "): I/O error while reading chunk data.");
            LogManager.logServerError(serverId, "STORE_CHUNK_FAILED", "I/O error while storing chunk.");
            return false;
        }
    }

    /**
     * Retrieves a specific file chunk from this simulated file server.
     * This method is now named 'retrieveChunk' and returns an InputStream.
     *
     * @param fileId The ID of the file.
     * @param chunkNumber The number of the chunk.
     * @return The chunk data as an InputStream, or null if not found.
     */
    public InputStream retrieveChunk(int fileId, int chunkNumber) {
        try {
            // Simulate network delay
            Thread.sleep(50);

            Map<Integer, Map<Integer, byte[]>> fileChunks = serverStorage.get(serverId);
            if (fileChunks != null && fileChunks.containsKey(fileId)) {
                byte[] chunkData = fileChunks.get(fileId).get(chunkNumber);
                if (chunkData != null) {
                    return new ByteArrayInputStream(chunkData);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogManager.logServerError(serverId, "GET_CHUNK_FAILED", "Interrupted while retrieving chunk.");
        }
        System.err.println(String.format("FileServerClient (Server %d): Chunk %d for file %d not found.",
                                         serverId, chunkNumber, fileId));
        LogManager.logServerError(serverId, "GET_CHUNK_FAILED",
                                  String.format("Chunk %d for file %d not found.", chunkNumber, fileId));
        return null;
    }

    /**
     * Deletes a specific file chunk from this server.
     *
     * @param fileId The ID of the file.
     * @param chunkNumber The number of the chunk to delete.
     * @return true if deletion was successful, false otherwise.
     */
    public boolean deleteChunk(int fileId, int chunkNumber) {
        Map<Integer, Map<Integer, byte[]>> fileChunks = serverStorage.get(serverId);
        if (fileChunks != null && fileChunks.containsKey(fileId) && fileChunks.get(fileId).containsKey(chunkNumber)) {
            fileChunks.get(fileId).remove(chunkNumber);
            System.out.println(String.format("FileServerClient (Server %d): Deleted chunk %d for file %d.",
                                             serverId, chunkNumber, fileId));
            LogManager.logServerAction(serverId, "DELETE_CHUNK",
                                       String.format("Chunk %d for file %d deleted.", chunkNumber, fileId));
            return true;
        }
        System.err.println(String.format("FileServerClient (Server %d): Chunk %d for file %d not found for deletion.",
                                         serverId, chunkNumber, fileId));
        LogManager.logServerError(serverId, "DELETE_CHUNK_FAILED",
                                  String.format("File %d, Chunk %d not found for deletion.", fileId, chunkNumber));
        return false;
    }

    /**
     * Clears all chunks associated with a specific file from this server.
     * This might be called when a file is deleted from the system.
     *
     * @param fileId The ID of the file to clear chunks for.
     * @return true if chunks were successfully cleared, false otherwise.
     */
    public boolean deleteAllFileChunks(int fileId) {
        if (serverStorage.get(serverId).remove(fileId) != null) {
            System.out.println(String.format("FileServerClient (Server %d): All chunks for file %d cleared.",
                                             serverId, fileId));
            LogManager.logServerAction(serverId, "CLEAR_FILE_CHUNKS",
                                       String.format("All chunks for file %d cleared.", fileId));
            return true;
        }
        System.err.println(String.format("FileServerClient (Server %d): No chunks found for file %d to clear.",
                                         serverId, fileId));
        LogManager.logServerError(serverId, "CLEAR_FILE_CHUNKS_FAILED",
                                  String.format("No chunks found for file %d to clear.", fileId));
        return false;
    }
}
