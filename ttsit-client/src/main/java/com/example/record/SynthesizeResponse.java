package com.example.record;

import java.util.List;

/**
 * Body of a POST /synthesize response from the TTS server.
 *
 * @param sample_rate  sample rate of the audio in Hz
 * @param pcm16_base64 base64-encoded 16-bit mono PCM samples
 * @param duration_sec total duration of the audio in seconds
 * @param words        per-word timing within the audio
 */
public record SynthesizeResponse(int sample_rate, String pcm16_base64, float duration_sec, List<WordTiming> words) {}
