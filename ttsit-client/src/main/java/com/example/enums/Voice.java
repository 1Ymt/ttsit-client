package com.example.enums;

/**
 * Voices available in ttsit/voices/voices (one constant per .pt file).
 * {@link #getId()} returns the file name without extension, which is the value
 * the TTS server expects in the "voice" field.
 */

//TODO: Refactor to use dynamic voice list from server instead of hardcoding voices here. This will allow for easier updates and additions of new voices without needing to recompile the client.

public enum Voice {
    AF_ALLOY("af_alloy"),
    AF_AOEDE("af_aoede"),
    AF_BELLA("af_bella"),
    AF_HEART("af_heart"),
    AF_JESSICA("af_jessica"),
    AF_KORE("af_kore"),
    AF_NICOLE("af_nicole"),
    AF_NOVA("af_nova"),
    AF_RIVER("af_river"),
    AF_SARAH("af_sarah"),
    AF_SKY("af_sky"),

    AM_ADAM("am_adam"),
    AM_ECHO("am_echo"),
    AM_ERIC("am_eric"),
    AM_FENRIR("am_fenrir"),
    AM_LIAM("am_liam"),
    AM_MICHAEL("am_michael"),
    AM_ONYX("am_onyx"),
    AM_PUCK("am_puck"),
    AM_SANTA("am_santa"),

    BF_ALICE("bf_alice"),
    BF_EMMA("bf_emma"),
    BF_ISABELLA("bf_isabella"),
    BF_LILY("bf_lily"),

    BM_DANIEL("bm_daniel"),
    BM_FABLE("bm_fable"),
    BM_GEORGE("bm_george"),
    BM_LEWIS("bm_lewis"),

    EF_DORA("ef_dora"),

    EM_ALEX("em_alex"),
    EM_SANTA("em_santa"),

    FF_SIWIS("ff_siwis"),

    HF_ALPHA("hf_alpha"),
    HF_BETA("hf_beta"),

    HM_OMEGA("hm_omega"),
    HM_PSI("hm_psi"),

    IF_SARA("if_sara"),

    IM_NICOLA("im_nicola"),

    JF_ALPHA("jf_alpha"),
    JF_GONGITSUNE("jf_gongitsune"),
    JF_NEZUMI("jf_nezumi"),
    JF_TEBUKURO("jf_tebukuro"),

    JM_KUMO("jm_kumo"),

    PF_DORA("pf_dora"),

    PM_ALEX("pm_alex"),
    PM_SANTA("pm_santa"),

    ZF_XIAOBEI("zf_xiaobei"),
    ZF_XIAONI("zf_xiaoni"),
    ZF_XIAOXIAO("zf_xiaoxiao"),
    ZF_XIAOYI("zf_xiaoyi"),

    ZM_YUNJIAN("zm_yunjian"),
    ZM_YUNXI("zm_yunxi"),
    ZM_YUNXIA("zm_yunxia"),
    ZM_YUNYANG("zm_yunyang");

    private final String id;

    Voice(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
