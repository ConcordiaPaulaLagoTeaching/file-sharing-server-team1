package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        // Initialize the FileSystemManager
        FileSystemManager fsManager = new FileSystemManager(fileSystemName,
                10 * 128);
        this.fsManager = fsManager;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                try (
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received from client: " + line);
                        String[] parts = line.split(" ");
                        String command = parts[0].toUpperCase();

                        switch (command) {
                            case "CREATE":
                                try {
                                    if (parts.length < 2) {
                                        writer.println("ERROR: CREATE command requires a filename.");
                                    } else {
                                        fsManager.createFile(parts[1]);
                                        writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                    }
                                } catch (IllegalArgumentException | IllegalStateException e) {
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Failed to create file: " + e.getMessage());
                                }
                                writer.flush();
                                break;
                            case "WRITE":
                                try {
                                    if (parts.length < 3) {
                                        writer.println("ERROR: WRITE command requires a filename and content.");
                                    } else {
                                        // Join all parts after the filename as content (in case content has spaces)
                                        StringBuilder contentBuilder = new StringBuilder();
                                        for (int i = 2; i < parts.length; i++) {
                                            if (i > 2) {
                                                contentBuilder.append(" ");
                                            }
                                            contentBuilder.append(parts[i]);
                                        }
                                        String content = contentBuilder.toString();
                                        fsManager.writeFile(parts[1], content);
                                        writer.println("SUCCESS: Content written to file '" + parts[1] + "'.");
                                    }
                                } catch (IllegalArgumentException | IllegalStateException e) {
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Failed to write file: " + e.getMessage());
                                }
                                writer.flush();
                                break;
                            // TODO: Implement other commands READ, WRITE, DELETE, LIST
                            case "QUIT":
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            default:
                                writer.println("ERROR: Unknown command.");
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

}
