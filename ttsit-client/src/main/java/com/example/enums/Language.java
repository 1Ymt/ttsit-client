package com.example.enums;

/**
 * Languages supported by the TTS server (see LANG_MAP in ttsserver.py).
 * {@link #getTag()} returns the BCP-47 tag the server expects in the "lang" field.
 */
public enum Language {
    ENGLISH("English", "en"),
    ENGLISH_US("English (US)", "en-us"),
    ENGLISH_GB("English (GB)", "en-gb"),
    SPANISH("Spanish", "es"),
    FRENCH("French", "fr"),
    HINDI("Hindi", "hi"),
    ITALIAN("Italian", "it"),
    JAPANESE("Japanese", "ja"),
    PORTUGUESE("Portuguese", "pt"),
    PORTUGUESE_BR("Portuguese (BR)", "pt-br"),
    CHINESE("Chinese", "zh");

    private final String displayName;
    private final String tag;

    Language(String displayName, String tag) {
        this.displayName = displayName;
        this.tag = tag;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
