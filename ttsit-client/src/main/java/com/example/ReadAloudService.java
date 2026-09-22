package com.example;

import java.util.concurrent.BlockingQueue;

import com.example.gui.WordHighlighter;
import com.example.record.SpokenSentence;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class ReadAloudService extends javafx.concurrent.Service<Void> {

    private AudioPlayer audioPlayer;
    private WordHighlighter highlighter;
    private BlockingQueue<SpokenSentence> audioQueue;

    private long runId;

    public ReadAloudService(AudioPlayer audioPlayer, WordHighlighter highlighter) {
        this.audioPlayer = audioPlayer;
        this.highlighter = highlighter;
    }
    
    public void setup(BlockingQueue<SpokenSentence> queue) {
        this.audioQueue = queue;
        runId++;
    }

    @Override
    protected Task<Void> createTask() {
        if (audioQueue == null) {
            throw new IllegalStateException("audioQueue is null.");
        }

        final BlockingQueue<SpokenSentence> queue = audioQueue;
        final long currentRunId = runId;
        
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                //decoding raw audio string to byte and reading it
                SpokenSentence spoken;
                audioPlayer.createLine();
                audioPlayer.start();
                while ((spoken = queue.take()) != SpokenSentence.DONE) {
                    if (isCancelled())
                        break;
                    final SpokenSentence current = spoken;
                    final double lead = audioPlayer.bufferedSeconds();

                    Platform.runLater(() -> highlighter.startHighlighting(current, lead));
                    audioPlayer.read(current.pcm());
                }
                audioPlayer.finish();

                return null;
            }

            @Override
            protected void cancelled() {
                if (currentRunId != runId)
                    return;
                highlighter.stop();

                /* finish() may have closed the line already, which for cleanup is a success. */
                if (audioPlayer.isOpen()) {
                    audioPlayer.cancel();
                }
            }
        };
    }
}
