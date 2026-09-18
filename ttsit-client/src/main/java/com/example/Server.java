package com.example;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

//TODO: Add periodic health check to see if the server is still running

public class Server {

    private int port;
    private String base;

    private ProcessBuilder processBuilder;
    private Process process;
    private HttpClient client;
    
    
    public Server() {
        this.port = freePort();
        File pythonAppDir = findPythonAppDir();
        File pythonExecutable = findVenvPython(pythonAppDir);

        try {
            this.processBuilder = createServer(pythonExecutable.getAbsolutePath(), port, pythonAppDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.base = "http://127.0.0.1:" + port;
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public void runServer() throws IOException, InterruptedException {
        this.process = processBuilder.start();
        waitUntilHealthy(client, base, process, Duration.ofSeconds(15));
        System.out.println("[java] server is up");
    }

    public void stopServer() throws InterruptedException {
        try {
            process.destroy(); // polite: SIGTERM / close
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly(); // impolite, but it's gone
                System.out.println("[java] python was forcibly stopped");
            }
            System.out.println("[java] python stopped, exit code " + process.exitValue());
        } finally {
            stopStdStream();
        }
        process = null;
    }
    
    private void stopStdStream() {
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public HttpResponse<String> sendSynthesizeRequest(String body)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(base + "/synthesize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    private void waitUntilHealthy(HttpClient client, String base, Process proc, Duration timeout)
            throws InterruptedException {
        var health = HttpRequest.newBuilder(URI.create(base + "/health")).GET().build();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!proc.isAlive()) {
                throw new IllegalStateException("python exited early with code " + proc.exitValue());
            }
            try {
                if (client.send(health, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                continue; 
            }
            Thread.sleep(250);
        }
        stopStdStream();
        throw new IllegalStateException("server did not become healthy within " + timeout);
    }
    
    private static ProcessBuilder createServer(String serverExecutable, int port, File pythonAppDir) throws IOException {
        ProcessBuilder process = new ProcessBuilder(serverExecutable, "ttsserver.py", "--port", String.valueOf(port))
                .directory(pythonAppDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        
        return process;
    }

    // Finds the PythonApp folder by walking up from the current working
    // directory, since that directory differs depending on how the app is run
    // (VS Code Run, terminal, mvnw, ...).
    private static File findPythonAppDir() {
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            File candidate = new File(dir, "ttsit");
            if (candidate.isDirectory()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("Could not find ttsit directory");
    }

    // Uses the venv's own Python interpreter instead of the "python" on PATH,
    // so packages installed into the venv (e.g. fastapi, uvicorn) are found.
    private static File findVenvPython(File pythonAppDir) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        File executable = isWindows
                ? new File(pythonAppDir, "venv/Scripts/python.exe")
                : new File(pythonAppDir, "venv/bin/python");
        if (!executable.isFile()) {
            throw new IllegalStateException("Could not find venv Python at " + executable);
        }
        return executable;
    }

    private int freePort() {
        try (var s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean isRunning() {
        return process != null && process.isAlive();
    }
}
