package com.example.record;

/**
 * Timing of a single word within a synthesized sentence.
 *
 * @param start    char offset within the request text, inclusive
 * @param end      char offset within the request text, exclusive
 * @param from_sec start of the word's audio, in seconds
 * @param to_sec   end of the word's audio, in seconds
 */
public record WordTiming(int start, int end, float from_sec, float to_sec) {}
