package com.example.gui;

import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Motion for the GUI, kept out of the controller so the controller stays free of behaviour.
 *
 * Timing rules used throughout:
 *   - entering elements decelerate into place (ease-out), leaving ones accelerate away (ease-in),
 *     on-screen changes use a symmetric curve;
 *   - micro feedback 60-120 ms, UI transitions 220-320 ms, groups revealed with a 70 ms stagger;
 *   - only opacity, translate and scale are animated, never layout properties.
 *
 * Start the app with -Dgui.reducedMotion=true to collapse every animation to an instant change.
 */
public final class Animations {

    private static final boolean REDUCED_MOTION = Boolean.getBoolean("gui.reducedMotion");

    /** Enter: fast start, gentle settle (cubic-bezier 0.16, 1, 0.3, 1). */
    public static final Interpolator ENTER = Interpolator.SPLINE(0.16, 1, 0.3, 1);
    /** Exit: gentle start, fast end (cubic-bezier 0.7, 0, 0.84, 0). */
    public static final Interpolator EXIT = Interpolator.SPLINE(0.7, 0, 0.84, 0);
    /** Move or change in place: symmetric (cubic-bezier 0.65, 0, 0.35, 1). */
    public static final Interpolator MOVE = Interpolator.SPLINE(0.65, 0, 0.35, 1);

    private static final Duration INSTANT = millis(60);
    private static final Duration FAST = millis(120);
    private static final Duration NORMAL = millis(220);
    private static final Duration SLOW = millis(320);
    private static final Duration STAGGER = millis(70);
    private static final Duration BREATH = millis(900);

    private static final double HOVER_SCALE = 1.02;
    private static final double PRESSED_SCALE = 0.97;
    private static final double RISE_DISTANCE = 10;

    private static final String SCALE_KEY = "animations.scale";
    private static final String BUSY_KEY = "animations.busy";

    private Animations() {
    }

    // ------------------------------------------------------------------ entrance

    /**
     * Reveals the window content group by group, each fading in and rising into
     * place, 70 ms after the previous one. Nodes start invisible so nothing
     * flashes before the stage is shown; the animation waits for the window.
     */
    public static void playEntrance(Node[]... groups) {
        for (Node[] group : groups) {
            for (Node node : group) {
                node.setOpacity(0);
                node.setTranslateY(RISE_DISTANCE);
            }
        }
        whenShown(groups[0][0], () -> {
            ParallelTransition all = new ParallelTransition();
            for (int i = 0; i < groups.length; i++) {
                for (Node node : groups[i]) {
                    ParallelTransition rise = riseIn(node, SLOW);
                    rise.setDelay(STAGGER.multiply(i));
                    all.getChildren().add(rise);
                }
            }
            all.play();
        });
    }

    // ------------------------------------------------------------------ buttons

    /**
     * Press feedback: a slight grow on hover, a quick dip while armed, and an
     * eased return. Scale only, so layout is never touched.
     */
    public static void addPressFeedback(Button... buttons) {
        for (Button button : buttons) {
            button.hoverProperty().addListener((obs, was, hovering) -> settleScale(button));
            button.armedProperty().addListener((obs, was, armed) -> settleScale(button));
        }
    }

    private static void settleScale(Button button) {
        double target = button.isArmed() ? PRESSED_SCALE : button.isHover() ? HOVER_SCALE : 1.0;
        Duration duration = button.isArmed() ? INSTANT : FAST;

        ScaleTransition previous = (ScaleTransition) button.getProperties().get(SCALE_KEY);
        if (previous != null) {
            previous.stop();
        }
        ScaleTransition scale = new ScaleTransition(duration, button);
        scale.setToX(target);
        scale.setToY(target);
        scale.setInterpolator(ENTER);
        button.getProperties().put(SCALE_KEY, scale);
        scale.play();
    }

    // ------------------------------------------------------------------ server status

    /**
     * Swaps the status text with a crossfade: the old text fades out, the change
     * is applied, the new text fades in, and the tally lamp pops once so the
     * eye is drawn to it. Before the window is showing the change is instant.
     */
    public static void transitionStatus(Node statusBox, Label statusText, Runnable applyChange) {
        if (REDUCED_MOTION || !isShowing(statusText)) {
            applyChange.run();
            return;
        }
        Node lamp = statusBox.lookup(".tally-lamp");

        FadeTransition out = new FadeTransition(FAST, statusText);
        out.setToValue(0);
        out.setInterpolator(EXIT);
        out.setOnFinished(e -> {
            applyChange.run();
            FadeTransition in = new FadeTransition(NORMAL, statusText);
            in.setToValue(1);
            in.setInterpolator(ENTER);
            in.play();
            if (lamp != null) {
                pop(lamp);
            }
        });
        out.play();
    }

    /**
     * While the server is starting or closing the lamp breathes slowly, which
     * reads as "working" without demanding attention. Stops when busy is false.
     */
    public static void setLampBusy(Node statusBox, boolean busy) {
        Node lamp = statusBox.lookup(".tally-lamp");
        if (lamp == null) {
            return;
        }
        FadeTransition current = (FadeTransition) lamp.getProperties().get(BUSY_KEY);
        if (busy) {
            if (current != null || REDUCED_MOTION) {
                return;
            }
            FadeTransition breathe = new FadeTransition(BREATH, lamp);
            breathe.setFromValue(1);
            breathe.setToValue(0.35);
            breathe.setCycleCount(Animation.INDEFINITE);
            breathe.setAutoReverse(true);
            breathe.setInterpolator(MOVE);
            lamp.getProperties().put(BUSY_KEY, breathe);
            breathe.play();
        } else if (current != null) {
            current.stop();
            lamp.getProperties().remove(BUSY_KEY);
            lamp.setOpacity(1);
        }
    }

    /**
     * Error feedback: a short horizontal shake, 400 ms in total, that settles
     * back to rest. Used when the server fails so the status is not missed.
     */
    public static void shake(Node node) {
        if (REDUCED_MOTION) {
            return;
        }
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), 0, MOVE)),
                new KeyFrame(Duration.millis(80), new KeyValue(node.translateXProperty(), -6, MOVE)),
                new KeyFrame(Duration.millis(160), new KeyValue(node.translateXProperty(), 6, MOVE)),
                new KeyFrame(Duration.millis(240), new KeyValue(node.translateXProperty(), -4, MOVE)),
                new KeyFrame(Duration.millis(320), new KeyValue(node.translateXProperty(), 3, MOVE)),
                new KeyFrame(Duration.millis(400), new KeyValue(node.translateXProperty(), 0, MOVE)));
        shake.play();
    }

    private static void pop(Node node) {
        ScaleTransition pop = new ScaleTransition(FAST, node);
        pop.setFromX(1);
        pop.setFromY(1);
        pop.setToX(1.4);
        pop.setToY(1.4);
        pop.setCycleCount(2);
        pop.setAutoReverse(true);
        pop.setInterpolator(MOVE);
        pop.play();
    }

    // ------------------------------------------------------------------ transcript

    /**
     * Adds a received line to the transcript. The line rises into place and the
     * view scrolls to it once the new height is known.
     */
    public static void appendReceived(ScrollPane scrollPane, TextFlow flow, String text) {
        Text line = new Text(text + System.lineSeparator());
        line.getStyleClass().add("text");
        line.setOpacity(0);
        line.setTranslateY(RISE_DISTANCE);
        flow.getChildren().add(line);

        riseIn(line, SLOW).play();

        Platform.runLater(() -> {
            scrollPane.applyCss();
            scrollPane.layout();
            Timeline scroll = new Timeline(
                    new KeyFrame(NORMAL, new KeyValue(scrollPane.vvalueProperty(), 1.0, MOVE)));
            scroll.play();
        });
    }

    /**
     * Clears the transcript: every line fades out together (ease-in, 120 ms),
     * then all are removed so the placeholder can return.
     */
    public static void clearReceived(TextFlow flow) {
        if (flow.getChildren().isEmpty()) {
            return;
        }
        ParallelTransition out = new ParallelTransition();
        for (Node line : flow.getChildren()) {
            FadeTransition fade = new FadeTransition(FAST, line);
            fade.setToValue(0);
            fade.setInterpolator(EXIT);
            out.getChildren().add(fade);
        }
        out.setOnFinished(e -> flow.getChildren().clear());
        out.play();
    }

    // ------------------------------------------------------------------ helpers

    private static ParallelTransition riseIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(1);
        fade.setInterpolator(ENTER);
        TranslateTransition rise = new TranslateTransition(duration, node);
        rise.setToY(0);
        rise.setInterpolator(ENTER);
        return new ParallelTransition(node, fade, rise);
    }

    private static Duration millis(double value) {
        return REDUCED_MOTION ? Duration.ONE : Duration.millis(value);
    }

    private static boolean isShowing(Node node) {
        Scene scene = node.getScene();
        return scene != null && scene.getWindow() != null && scene.getWindow().isShowing();
    }

    /** Runs the action once the window that holds the node is showing, immediately if it already is. */
    private static void whenShown(Node node, Runnable action) {
        once(node.sceneProperty(), scene -> scene != null, scene ->
                once(scene.windowProperty(), window -> window != null, window ->
                        once(window.showingProperty(), showing -> showing, showing -> action.run())));
    }

    /** Runs the action once the value satisfies the condition, immediately if it already does. */
    private static <T> void once(ObservableValue<T> value, Predicate<T> ready, Consumer<T> action) {
        T current = value.getValue();
        if (ready.test(current)) {
            action.accept(current);
            return;
        }
        value.addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> obs, T was, T now) {
                if (ready.test(now)) {
                    value.removeListener(this);
                    action.accept(now);
                }
            }
        });
    }
}
