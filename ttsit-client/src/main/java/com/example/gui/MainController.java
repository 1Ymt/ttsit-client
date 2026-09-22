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
import com.example.record.SentenceRef;

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
import javafx.scene.shape.Path;
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
    private Path transcriptHighlight;

    @FXML
    private Label speechEyebrow;

    @FXML
    private VBox speechControls;

    @FXML
    private Label readAloudHint;

    @FXML
    private Button readAloudButton;

    @FXML
    private ComboBox<Voice> voiceComboBox;

    @FXML
    private Spinner<Double> speedSpinner;

    @FXML
    private ComboBox<Language> languageComboBox;

    private static final String HINT_SERVER_OFFLINE = "Start the server first.";
    private static final String HINT_TRANSCRIPT_EMPTY = "Nothing to read. Send text first.";

    private Server server;
    private ReadAloudService service;
    private ReadAloudSession session;
    private WordHighlighter wordHighlighter;

    @FXML
    private void initialize() {
        init();
        this.server = new Server();

        AudioPlayer audioPlayer = new AudioPlayer();
        this.wordHighlighter = new WordHighlighter(receivedTextFlow, transcriptHighlight);
        this.service = new ReadAloudService(audioPlayer, wordHighlighter);
        this.session = new ReadAloudSession(server, service);
    }
    
    private void init() {
        voiceComboBox.getItems().setAll(Voice.values());
        languageComboBox.getItems().setAll(Language.values());

        voiceComboBox.getSelectionModel().selectFirst();
        languageComboBox.getSelectionModel().selectFirst();
        speedSpinner.getValueFactory().setConverter(speedFormat);
        
        speedSpinner.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused) {
                speedSpinner.commitValue();
            }
        });

        textEyebrow.setLabelFor(inputTextArea);
        transcriptEyebrow.setLabelFor(transcriptScrollPane);
        speechEyebrow.translateYProperty().bind(speechEyebrow.heightProperty().divide(-2));

        transcriptPlaceholder.visibleProperty().bind(Bindings.isEmpty(receivedTextFlow.getChildren()));

        /* rangeShape() measures from the flow's content box, the padding is not in it. */
        transcriptHighlight.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> receivedTextFlow.getInsets().getLeft(), receivedTextFlow.insetsProperty()));
        transcriptHighlight.layoutYProperty().bind(Bindings.createDoubleBinding(
                () -> receivedTextFlow.getInsets().getTop(), receivedTextFlow.insetsProperty()));

        clearTranscriptButton.disableProperty().bind(Bindings.isEmpty(receivedTextFlow.getChildren()));

        setServerState(ServerState.CLOSED);

        Animations.addPressFeedback(startServerButton, stopServerButton, sendButton, readAloudButton,
                clearTranscriptButton);
        Animations.playEntrance(
                new Node[] {serverRail},
                new Node[] {textEyebrow, inputTextArea, sendButton},
                new Node[] {transcriptHeader, transcriptScrollPane, speechControls });
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
                return speedSpinner.getValue();
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

        startServerButton.setDisable(state != ServerState.CLOSED && state != ServerState.FAILED);
        stopServerButton.setDisable(state != ServerState.RUNNING);

        if (state == ServerState.RUNNING && HINT_SERVER_OFFLINE.equals(readAloudHint.getText())) {
            Animations.hideHint(readAloudHint);
        }
    }

    public void appendReceived(String text) {
        Animations.appendReceived(transcriptScrollPane, receivedTextFlow, text);
        if (!text.isBlank() && HINT_TRANSCRIPT_EMPTY.equals(readAloudHint.getText())) {
            Animations.hideHint(readAloudHint);
        }
    }

    private void refuseReadAloud(String reason) {
        Animations.shake(speechControls);
        Animations.showHint(readAloudHint, reason);
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
        String inputText = inputTextArea.getText();
        if (!inputText.isBlank()) {
            appendReceived(inputTextArea.getText());
        }
    }

    @FXML
    private void onReadAloud() throws IOException, InterruptedException, LineUnavailableException {
        if (!server.isRunning()) {
            refuseReadAloud(HINT_SERVER_OFFLINE);
            return;
        }

        List<String> text_list = getTextFromFlowtext();
        if (text_list.isEmpty()) {
            refuseReadAloud(HINT_TRANSCRIPT_EMPTY);
            return;
        }
        Animations.hideHint(readAloudHint);
        List<Sentence> sentences = splitSentence(text_list);

        session.start(sentences, voiceComboBox.getValue(), speedSpinner.getValue().floatValue(), languageComboBox.getValue());

        service.setOnRunning(e -> readAloudButton.setDisable(true));
        service.setOnSucceeded(e -> readAloudButton.setDisable(false));
        service.setOnFailed(e -> {
            readAloudButton.setDisable(false);
            Animations.shake(speechControls);
        });

        service.setOnCancelled(e -> readAloudButton.setDisable(false));
    }
    
    private List<Sentence> splitSentence(List<String> textList) {
        List<Sentence> sentences = new ArrayList<>();

        for (int i = 0; i < textList.size(); i++) {
            String text = textList.get(i);
            BreakIterator it = BreakIterator.getSentenceInstance(Locale.forLanguageTag(languageComboBox.getValue().getTag())); // locale from dc:language
            it.setText(text);
            int start = it.first();
            for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
                String s = text.substring(start, end).strip();
                if (!s.isEmpty()) {
                    int offset = text.indexOf(s, start);
                    sentences.add(new Sentence(new SentenceRef(i, offset), s));
                }
            }
        }        
        return sentences;
    }

    private List<String> getTextFromFlowtext() {
        List<String> text_string = new ArrayList<>();
        List<Node> children = receivedTextFlow.getChildren();
        for (Node node : children) {
            if (node instanceof Text) {
                text_string.add(((Text) node).getText());
            }
        }
        return text_string;
    }

    @FXML
    private void onClearTranscript() {
        Animations.clearReceived(receivedTextFlow);
        session.cancel();
    }
}
