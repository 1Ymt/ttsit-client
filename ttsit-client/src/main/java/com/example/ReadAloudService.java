package com.example;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.example.enums.Language;
import com.example.enums.Voice;
import com.example.record.Sentence;
import com.example.record.SynthesizeRequest;
import com.example.record.SynthesizeResponse;
import com.google.gson.Gson;

import javafx.concurrent.Task;

public class ReadAloudService extends javafx.concurrent.Service<Void> {
    
    private static final SynthesizeResponse DONE = new SynthesizeResponse(-1, null);

    private List<Sentence> sentences;
    private Voice voice;
    private float speed;
    private Language language;

    private Server server;
    private AudioPlayer audioPlayer;
    private Gson gson;

    public ReadAloudService(Server server, AudioPlayer audioPlayer) {
        this(server, audioPlayer, null, null, 1.0f, Language.ENGLISH);
    }

    public ReadAloudService(Server server, AudioPlayer audioPlayer, List<Sentence> sentences, Voice voice, float speed,
            Language language) {
        this.server = server;
        this.audioPlayer = audioPlayer;

        this.sentences = sentences;
        this.voice = voice;
        this.speed = speed;
        this.language = language;

        this.gson = new Gson();
    }

    @Override
    protected Task<Void> createTask() {
        if (sentences == null || voice == null)
            throw new IllegalStateException("sentences or voice are null");

        List<Sentence> snapshot = this.sentences;
        String voice_id = this.voice.getId();
        float speed = this.speed;
        String language_tag = this.language.getTag();

        BlockingQueue<SynthesizeResponse> audioQueue = new LinkedBlockingQueue<>(5);
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                //Fetching raw audio string from tts server and putting it into queue
                Thread fetchResponse = new Thread(() -> {
                    try {
                        for (Sentence sentence : snapshot) {
                            if (isCancelled())
                                break;
                            SynthesizeRequest synthesizeRequest = new SynthesizeRequest(sentence.text(), voice_id,
                                    speed, language_tag);
                            String body = gson.toJson(synthesizeRequest);

                            HttpResponse<String> raw_response = server.sendSynthesizeRequest(body);
                            SynthesizeResponse response = gson.fromJson(raw_response.body(), SynthesizeResponse.class);

                            audioQueue.put(response);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    } finally {
                        audioQueue.offer(DONE);
                    }
                }, "fetch-audio");
                fetchResponse.setDaemon(true);
                fetchResponse.start();

                //decoding raw audio string to byte and reading it
                SynthesizeResponse response;
                while ((response = audioQueue.take()) != DONE) {
                    if (isCancelled())
                        break;
                    audioPlayer.read(response.pcm16_base64());
                }
                return null;
            }

            @Override
            protected void cancelled() {
                audioPlayer.stop();
                audioQueue.clear();
            }
        };
    }

   

    public void setup(List<Sentence> sentences, Voice voice, Float speed, Language language) {
        this.sentences = sentences;
        this.voice = voice;
        this.speed = speed;
        this.language = language;
    }
    
    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public Voice getVoice() {
        return voice;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

}
