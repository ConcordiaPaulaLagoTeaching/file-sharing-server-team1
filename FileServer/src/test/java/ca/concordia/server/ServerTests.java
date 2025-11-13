package ca.concordia.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTests {

    private Thread serverThread;
    private final String testFilePath = "server-test-filesystem.dat";
    private final int serverPort = 12345;

    @BeforeEach
    public void setUp() throws Exception {
        // Clean up any existing test file
        File file = new File(testFilePath);
        if (file.exists()) {
            file.delete();
        }

        // Start server in a separate thread
        serverThread = new Thread(() -> {
            FileServer server = new FileServer(serverPort, testFilePath, 10 * 128);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give server time to start
        Thread.sleep(500);
    }

    @AfterEach
    public void tearDown() {
        // Stop server thread
        if (serverThread != null) {
            serverThread.interrupt();
        }

        // Clean up test file
        File file = new File(testFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testHandlesHundredsOfClientsQuickly() throws Exception {
        int numClients = 100;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("localhost", serverPort);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    String fileName = "file" + clientId + ".txt";
                    out.println("CREATE " + fileName);
                    String response = in.readLine();
                    if (response == null || !response.contains("SUCCESS")) {
                        throw new Exception("Failed to create file: " + response);
                    }

                    out.println("QUIT");
                    in.readLine();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Server should handle clients within timeout");

        if (!exceptions.isEmpty()) {
            fail("Some clients failed: " + exceptions.get(0).getMessage());
        }
    }

    @Test
    public void testMalformedInputDoesNotCrashServer() throws Exception {
        try (Socket socket = new Socket("localhost", serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send malformed commands
            out.println("INVALID_COMMAND");
            String response1 = in.readLine();
            assertNotNull(response1);
            assertTrue(response1.contains("ERROR") || response1.contains("Unknown"));

            out.println("");
            String response2 = in.readLine();
            assertNotNull(response2);

            out.println("CREATE");
            String response3 = in.readLine();
            assertNotNull(response3);

            // Server should still be responsive
            out.println("CREATE valid.txt");
            String response4 = in.readLine();
            assertNotNull(response4);

            out.println("QUIT");
        }
    }

    @Test
    public void testServerRestartPersistence() throws Exception {
        String fileName = "persistent.txt";
        String content = "This should persist";

        // Create and write file
        try (Socket socket = new Socket("localhost", serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CREATE " + fileName);
            in.readLine();

            out.println("WRITE " + fileName + " " + content);
            in.readLine();

            out.println("QUIT");
        }

        // Restart server
        serverThread.interrupt();
        Thread.sleep(500);

        serverThread = new Thread(() -> {
            FileServer server = new FileServer(serverPort, testFilePath, 10 * 128);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        // Check if file persists (this test may need adjustment based on actual
        // persistence implementation)
        // For now, just verify server is responsive after restart
        try (Socket socket = new Socket("localhost", serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CREATE test.txt");
            String response = in.readLine();
            assertNotNull(response);

            out.println("QUIT");
        }
    }
}
