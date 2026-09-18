package com.example.record;

/**
 * Body of a POST /synthesize request to the TTS server.
 *
 * @param text  the sentence to synthesize
 * @param voice the voice id, e.g. "af_heart"
 * @param speed playback speed multiplier
 * @param lang  BCP-47 language tag
 */
public record SynthesizeRequest(String text, String voice, float speed, String lang) {}