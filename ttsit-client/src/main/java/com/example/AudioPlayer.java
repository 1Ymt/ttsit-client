package com.example;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.example.exception.LineNotOpenException;

/*
    TODO: Sync word highlighting to real playback position instead of write time.
          1. Track writtenFrames in read() using write()'s return value (short writes after a flush).
          2. Add bufferedSeconds() = (writtenFrames - line.getLongFramePosition()) / sample rate,
             i.e. the lead time until the next write() becomes audible. Not framePosition alone,
             that counter only grows and would push the highlight further off with every sentence.
          3. Resync writtenFrames = getLongFramePosition() after cancel()'s flush(), since flushed
             frames are discarded and never counted as played.
          4. ReadAloudService: read bufferedSeconds() before read(), pass it to startHighlighting();
             WordHighlighter: take it as double (long / 24000 truncates to whole seconds).

    TODO: Other fixes from the review:
          - Move drain() out of read() into stop(). Per chunk it empties the buffer at every
            sentence boundary (underrun gap); in stop() it keeps stop() from clipping the tail.
            Step 2 above only does something once this is out of the way.
*/

public class AudioPlayer {

    private static final AudioFormat FORMAT = new AudioFormat(24_000f, 16, 1, true, false);

    // TODO: an AtomicReference taken with getAndSet(null) in finish()/cancel() would make the
    //       teardown exactly-once, so a cancel landing mid-finish() cannot slip past isOpen().
    private volatile SourceDataLine current;

    private long writtenFrames;
    
    public AudioPlayer() {}

    public void read(byte[] pcm) {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("Audio line is not open. Call createLine() before reading audio data.");

        int written = line.write(pcm, 0, pcm.length);
        writtenFrames += written / FORMAT.getFrameSize();

    }
    
    /** Whether a line is currently held, i.e. whether the other calls have something to act on. */
    public boolean isOpen() {
        return current != null;
    }

    public void createLine() throws LineUnavailableException {
        if (current != null) {
            cancel();
        }
        SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
        line.open(FORMAT);
        current = line;
    }

    public void start() {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("No audio line to start. Call createLine() first.");

        line.start();
    }
    
    public void stop() {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("No audio line to stop. Call createLine() first.");

        line.stop();
    }

    public void finish() {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("No audio line to finish. Call createLine() first.");

        // Safeguard if line was stopped and finished was called
        if (line.isRunning()) {
            line.drain();
            line.stop();
        } else {
            line.flush();
        }
        line.close();
        writtenFrames = 0;
        current = null;
    }
    
    public void cancel() {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("No audio line to cancel. Call createLine() first.");

        line.stop();
        line.flush();
        line.close();
        writtenFrames = 0;
        current = null;
    }

    public double bufferedSeconds() {
        SourceDataLine line = current;
        if (line == null)
            throw new LineNotOpenException("No audio line to measure. Call createLine() first.");

        long queued = writtenFrames - line.getLongFramePosition();
        return Math.max(0, queued) / FORMAT.getSampleRate();
    }
}
