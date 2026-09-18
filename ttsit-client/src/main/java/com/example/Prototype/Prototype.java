package com.example.Prototype;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.google.gson.Gson;
import com.example.record.SynthesizeRequest;
import com.example.record.SynthesizeResponse;

public class Prototype {
    public static void main(String[] args) throws IOException, InterruptedException, LineUnavailableException {
        // 1. Ask the OS for a free port, then release it again
        int port = freePort();
        System.out.println("[java] chose port " + port);

        //Get the path to the python executable in the venv and the python server directory
        File pythonAppDir = findPythonAppDir();
        File pythonExecutable = findVenvPython(pythonAppDir);

        // 2. Start the server in a child process, passing the port as an argument
        Process process = runServer(pythonExecutable.getAbsolutePath(), port, pythonAppDir);
        System.out.println("[java] started python, pid " + process.pid());

        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        String base = "http://127.0.0.1:" + port;

        try {
            // 3. Wait for the server to become healthy, or fail if it doesn't within 15 seconds
            waitUntilHealthy(client, base, process, Duration.ofSeconds(15));
            System.out.println("[java] server is up");

            // 4. Convert SynthesizeRequest object to JSON
            Gson gson = new Gson();
            String body = gson.toJson(new SynthesizeRequest(
                    "This body is the first one. The next one needs to wait for the buffer to be finished. a.k.a this needs to finish.",
                    "af_heart", 1.0f, "en"));

            // 5. Send SynthesizeRequest json as a request to the server and get the response from it
            HttpResponse<String> response = sendSynthesizeRequest(client, base, body);
            
            // 6. Outputs the response status code and convert the response body from JSON to SynthesizeResponse object
            System.out.println("[java] status " + response.statusCode());
            SynthesizeResponse res = gson.fromJson(response.body(), SynthesizeResponse.class);
            
            // 7. Decode the base64 audio string from the response and play it using the readaloud method
            readaloud(res.pcm16_base64());

        } finally {
            //  8. Stop the server process
            process.destroy(); // polite: SIGTERM / close
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly(); // impolite, but it's gone
            }
            System.out.println("[java] python stopped, exit code " + process.exitValue());
        }
    }

    static void readaloud(String base64_audio_str) throws LineUnavailableException {
        byte[] pcm = Base64.getDecoder().decode(base64_audio_str);  // decode base64 to PCM bytes
        AudioFormat fmt = new AudioFormat(24_000f, 16, 1, true, false);
        //                                              rate                    bits        mono       signed   little-endian

        SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
        line.open(fmt);
        try {
            line.start();
            line.write(pcm, 0, pcm.length);   // blocks while the sound card drains it
            line.drain();   // wait for the last samples to finish
        } finally {
            line.close();
        }
    }
    
    static HttpResponse<String> sendSynthesizeRequest(HttpClient client, String base, String body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(base + "/synthesize"))  // create a POST request to the /synthesize endpoint
                .header("Content-Type", "application/json") // set the content type to JSON
                .POST(HttpRequest.BodyPublishers.ofString(body))    // set the request body to the JSON string
                .build();
        System.out.println("[java] send Synthesize request");
        return client.send(request, HttpResponse.BodyHandlers.ofString());  // send the request and return the response
    }
    
    static void waitUntilHealthy(HttpClient client, String base, Process proc, Duration timeout) throws InterruptedException {
        var health = HttpRequest.newBuilder(URI.create(base + "/health")).GET().build(); // create a GET request to the /health endpoint
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {  // loop until the timeout is reached
            if (!proc.isAlive()) {
                throw new IllegalStateException("python exited early with code " + proc.exitValue());
            }
            try {
                if (client.send(health, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                continue;// "connection refused": server not listening yet. Normal. Try again.
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("server did not become healthy within " + timeout);
    }
    
    private static Process runServer(String serverExecutable, int port, File pythonAppDir) throws IOException {
        Process process = new ProcessBuilder(serverExecutable, "ttsserver.py", "--port", String.valueOf(port))
                .directory(pythonAppDir)
                .inheritIO().start();
        
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

    public static int freePort() throws IOException {
        try (var s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
