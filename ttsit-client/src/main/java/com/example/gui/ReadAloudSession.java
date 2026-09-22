package com.example.gui;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.example.ReadAloudService;
import com.example.Server;
import com.example.enums.Language;
import com.example.enums.Voice;
import com.example.record.Sentence;
import com.example.record.SpokenSentence;
import com.example.record.SynthesizeRequest;
import com.example.record.SynthesizeResponse;

public class ReadAloudSession {
    private Server server;
    private ReadAloudService service;

    private BlockingQueue<SpokenSentence> audioQueue;

    private Thread fetch;

    public ReadAloudSession(Server server, ReadAloudService service) {
        this.server = server;
        this.service = service;
    }

    public void start(List<Sentence> sentences, Voice voice, float speed, Language language) {
        cancel();
        if (sentences == null || voice == null)
            throw new IllegalStateException("sentences or voice are null");

        audioQueue = new LinkedBlockingQueue<>(5);
        service.setup(audioQueue);

        fetch = new Thread(() -> fetchAll(sentences, voice.getId(), speed, language.getTag()), "fetch-audio");
        fetch.setDaemon(true);
        fetch.start();

        service.restart();
    }
    
    public void cancel() {
        if (fetch != null) {
            fetch.interrupt();
        }
        service.cancel();
    }

    private void fetchAll(List<Sentence> sentences, String voice_id, float speed, String language_tag) {
        try {
            try {
                for (Sentence sentence : sentences) {
                    SynthesizeRequest synthesizeRequest = new SynthesizeRequest(sentence.text(), voice_id, speed,
                            language_tag);

                    SynthesizeResponse res = server.synthesize(synthesizeRequest);
                    SpokenSentence spokenSentence = new SpokenSentence(sentence.ref(),
                                                                    res.sample_rate(),
                                                                    decode_base64_to_byte(res.pcm16_base64()),
                                                                    res.duration_sec(),
                                                                    res.words());

                    audioQueue.put(spokenSentence);
                }
            } catch (IOException e) {
                //server can't synthesize
            } 
            audioQueue.offer(SpokenSentence.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private byte[] decode_base64_to_byte(String pcm16_base64) {
        return Base64.getDecoder().decode(pcm16_base64);
    }

    
}
