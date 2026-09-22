package com.example.record;

import java.util.List;

public record SpokenSentence(SentenceRef ref, int sample_rate, byte[] pcm, float duration_sec, List<WordTiming> words) {
    public static final SpokenSentence DONE = new SpokenSentence(null, 0, null, 0, null);
}
