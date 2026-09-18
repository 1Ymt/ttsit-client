package com.example.gui;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.LineUnavailableException;

import com.example.AudioPlayer;
import com.example.ReadAloudService;
import com.example.Server;
import com.example.enums.Language;
import com.example.enums.ServerState;
import com.example.enums.Voice;
import com.example.record.Sentence;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

public class MainController {

    @FXML
    private VBox serverRail;

    @FXML
    private Button startServerButton;

    @FXML
    private Button stopServerButton;

    @FXML
    private HBox serverStatusBox;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private Button sendButton;

    @FXML
    private Label textEyebrow;

    @FXML
    private HBox transcriptHeader;

    @FXML
    private Label transcriptEyebrow;

    @FXML
    private Button clearTranscriptButton;

    @FXML
    private ScrollPane transcriptScrollPane;

    @FXML
    private Label transcriptPlaceholder;

    @FXML
    private TextFlow receivedTextFlow;

    @FXML
    private Label speechEyebrow;

    @FXML
    private VBox speechControls;

    @FXML
    private Button readAloudButton;

    @FXML
    private ComboBox<Voice> voiceComboBox;

    @FXML
    private Spinner<Double> speedSpinner;

    @FXML
    private ComboBox<Language> languageComboBox;

    private Server server;
    private AudioPlayer audioPlayer;
    private ReadAloudService readAloudService;

    @FXML
    private void initialize() {
        init();
        this.server = new Server();
        this.audioPlayer = new AudioPlayer();
        this.readAloudService = new ReadAloudService(server, audioPlayer);
    }
    
    private void init() {
        voiceComboBox.getItems().setAll(Voice.values());
        languageComboBox.getItems().setAll(Language.values());
        // The first entry of each dropdown is preselected so a request can be sent right away.
        voiceComboBox.getSelectionModel().selectFirst();
        languageComboBox.getSelectionModel().selectFirst();
        speedSpinner.getValueFactory().setConverter(speedFormat);
        // A value typed into the spinner is committed when focus leaves it, not only on Enter.
        speedSpinner.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused) {
                speedSpinner.commitValue();
            }
        });

        // Zone labels name the control they sit above, so assistive tech reads them together.
        textEyebrow.setLabelFor(inputTextArea);
        transcriptEyebrow.setLabelFor(transcriptScrollPane);

        // Empty state: the placeholder is shown until the first reply arrives.
        transcriptPlaceholder.visibleProperty().bind(Bindings.isEmpty(receivedTextFlow.getChildren()));
        // Nothing to clear while the transcript is empty.
        clearTranscriptButton.disableProperty().bind(Bindings.isEmpty(receivedTextFlow.getChildren()));

        setServerState(ServerState.CLOSED);

        Animations.addPressFeedback(startServerButton, stopServerButton, sendButton, readAloudButton,
                clearTranscriptButton);
        Animations.playEntrance(
                new Node[] {serverRail},
                new Node[] {textEyebrow, inputTextArea, sendButton},
                new Node[] {transcriptHeader, transcriptScrollPane, speechEyebrow, speechControls });
    }

    /** Shows the speed as e.g. "1.0" so the readout has a stable width. */
    private final StringConverter<Double> speedFormat = new StringConverter<>() {
        @Override
        public String toString(Double value) {
            return value == null ? "" : String.format(java.util.Locale.ROOT, "%.1f", value);
        }

        @Override
        public Double fromString(String text) {
            try {
                return Double.parseDouble(text.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return speedSpinner.getValue();  // keep the last valid value instead of resetting
            }
        }
    };

    /**
     * Updates the status text and swaps the state style class on the status
     * box so theme.css can light the tally lamp accordingly.
     */
    public void setServerState(ServerState state) {
        Animations.transitionStatus(serverStatusBox, serverStatusLabel, () -> {
            serverStatusLabel.setText(state.getLabel());
            for (ServerState s : ServerState.values()) {
                serverStatusBox.getStyleClass().remove(s.getStyleClass());
            }
            serverStatusBox.getStyleClass().add(state.getStyleClass());
        });
        Animations.setLampBusy(serverStatusBox,
                state == ServerState.STARTING || state == ServerState.CLOSING);
        if (state == ServerState.FAILED) {
            Animations.shake(serverStatusBox);
        }

        // Only the action that makes sense in this state is available; both are off while busy.
        // After a failure the server can be started again.
        startServerButton.setDisable(state != ServerState.CLOSED && state != ServerState.FAILED);
        stopServerButton.setDisable(state != ServerState.RUNNING);
    }

    /** Appends a received line to the transcript; it rises into view and the transcript scrolls to it. */
    public void appendReceived(String text) {
        Animations.appendReceived(transcriptScrollPane, receivedTextFlow, text);
    }

    @FXML
    private void onStartServer() {
        Task<Void> checkHeatlh = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                server.runServer();
                return null;
            }
        };

        checkHeatlh.setOnRunning(e -> {
            setServerState(ServerState.STARTING);
        });

        checkHeatlh.setOnSucceeded(e -> {
            setServerState(ServerState.RUNNING);
        });

        checkHeatlh.setOnFailed(e -> {
            setServerState(ServerState.FAILED);
            checkHeatlh.getException().printStackTrace();
            Animations.shake(startServerButton);
        });

        Thread thread = new Thread(checkHeatlh, "health-check-server");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onStopServer() {

        Task<Void> stop = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                server.stopServer();
                return null;
            }
        };

        stop.setOnRunning(e -> {
            setServerState(ServerState.CLOSING);
        });

        stop.setOnSucceeded(e -> {
            setServerState(ServerState.CLOSED);
        });

        stop.setOnFailed(e -> {
            stop.getException().printStackTrace();
            Animations.shake(stopServerButton);
        });

        Thread thread = new Thread(stop, "stop-server");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onSend() {
        appendReceived(inputTextArea.getText());
    }

    @FXML
    private void onReadAloud() throws IOException, InterruptedException, LineUnavailableException {
        if (!server.isRunning()) {
            Animations.shake(speechControls);
            return;
        }
            
        String text = getTextFromFlowtext();
        List<Sentence> sentences = splitSentence(text);

        readAloudService.setup(sentences, voiceComboBox.getValue(), speedSpinner.getValue().floatValue(), languageComboBox.getValue());
        readAloudService.restart();

        readAloudService.setOnRunning(e -> readAloudButton.setDisable(true));
        readAloudService.setOnSucceeded(e -> readAloudButton.setDisable(false));
        readAloudService.setOnFailed(e -> {
            readAloudButton.setDisable(false);
            Animations.shake(speechControls);
        });

        readAloudService.setOnCancelled(e -> readAloudButton.setDisable(false));
    }
    
    private List<Sentence> splitSentence(String text) {
        List<Sentence> sentences = new ArrayList<>();

        BreakIterator it = BreakIterator.getSentenceInstance(Locale.forLanguageTag(languageComboBox.getValue().getTag())); // locale from dc:language
        it.setText(text);
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String s = text.substring(start, end).strip();
            if (!s.isEmpty())
                sentences.add(new Sentence(s));
        }
        return sentences;
    }

    private String getTextFromFlowtext() {
        String text = "";
        List<Node> children = receivedTextFlow.getChildren();
        for (Node node : children) {
            if (node instanceof Text) {
                text += ((Text) node).getText();
            }
        }
        return text;
    }

    @FXML
    private void onClearTranscript() {
        Animations.clearReceived(receivedTextFlow);
        readAloudService.cancel();
    }
}
