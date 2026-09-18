package com.example.record;

/**
 * Body of a POST /synthesize response from the TTS server.
 *
 * @param sample_rate  sample rate of the audio in Hz
 * @param pcm16_base64 base64-encoded 16-bit mono PCM samples
 */
public record SynthesizeResponse(int sample_rate, String pcm16_base64) {}
