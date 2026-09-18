package com.example;

import java.util.Base64;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer {

    private static final AudioFormat FORMAT = new AudioFormat(24_000f, 16, 1, true, false);

    private volatile SourceDataLine current;
    
    public AudioPlayer() {

    }

    public void read(String base64_audio_str) throws LineUnavailableException {
        byte[] pcm = Base64.getDecoder().decode(base64_audio_str);

        SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
        line.open(FORMAT);
        current = line;
        try {
            line.start();
            line.write(pcm, 0, pcm.length); // blocks while the sound card drains it
            line.drain(); // wait for the last samples to finish
        } finally {
            current = null;
            line.close();
        }
    }
    
    public void stop() {
        SourceDataLine line = current;
        if (line != null) {
            line.stop();
            line.flush();
        }
    }
}
