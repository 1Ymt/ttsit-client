package com.example.gui;

import java.util.ArrayList;
import java.util.List;

import com.example.record.SpokenSentence;
import com.example.record.WordTiming;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/*
    TODO: One Timeline per sentence instead of the shared current field.
          4. deselect(): only clear if the selection still matches this word's range.
             With the overlap, sentence n's trailing deselect can otherwise blank the
             first word of n+1 when both live in the same Text node.
*/

public class WordHighlighter {
    private List<Timeline> running;
    private final TextFlow textFlow;
    private final Path highlightPath;

    public WordHighlighter(TextFlow textFlow, Path highlightPath) {
        this.textFlow = textFlow;
        this.highlightPath = highlightPath;

        running = new ArrayList<>();
    }

    public void startHighlighting(SpokenSentence sentence, double lead) {
        Timeline timeline = new Timeline();

        int base = baseOffset(sentence.ref().blockIndex());

        timeline.setOnFinished(e -> {
            clear(timeline);
        });

        for (WordTiming timing : sentence.words()) {
            highlight(timeline, base, timing, sentence.ref().offset(), lead);
        }

        /* Without it the timeline would end on the last word instead of with the audio. */
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(sentence.duration_sec() + lead)));

        running.add(timeline);

        timeline.play();
    }

    public void stop() {
        for (Timeline timeline : running) {
            timeline.stop();
        }
        running.clear();
        hide();
    }

    private void highlight(Timeline timeline, int base, WordTiming timing, int offset, double lead) {
        KeyFrame frame = new KeyFrame(Duration.seconds(timing.from_sec() + lead), e -> select(base, timing, offset));
        timeline.getKeyFrames().add(frame);
    }

    private void clear(Timeline timeline) {
        timeline.getKeyFrames().clear();
        running.remove(timeline);

        if (running.isEmpty()) {
            hide();
        }
    }

    private void select(int base, WordTiming timing, int offset) {
        highlightPath.getElements().setAll(
                textFlow.rangeShape(base + offset + timing.start(), base + offset + timing.end()));
    }

    private void hide() {
        highlightPath.getElements().clear();
    }

    /* The flow lays out every child's text as one string, so the shape has to be asked for
       with an index into that string, not into the block the word belongs to. */
    private int baseOffset(int blockIndex) {
        List<Text> textNode = getTextNode(textFlow);
        int base = 0;

        for (int i = 0; i < blockIndex; i++) {
            base += textNode.get(i).getText().length();
        }
        return base;
    }

    private List<Text> getTextNode(TextFlow textFlow) {
        List<Text> textNode = new ArrayList<>();

        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                textNode.add((Text) node);
            }
        }
        return textNode;
    }


}
