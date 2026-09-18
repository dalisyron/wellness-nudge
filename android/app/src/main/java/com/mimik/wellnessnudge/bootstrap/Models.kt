package com.mimik.wellnessnudge.bootstrap

import com.mimik.mimoeclient.milm.model.ModelDownload

/**
 * Catalogue of the on-device models the app needs to run, with the user-
 * facing metadata the setup screen renders. IDs must match what the
 * wellness-nudge mim expects in INFERENCE_MODEL env / classifier calls.
 */
data class ModelSpec(
    val download: ModelDownload,
    /** Short user-facing role, e.g. "Nudge writer". */
    val displayName: String,
    /** Underlying model name + parameter count, e.g. "SmolLM2 360M". */
    val technicalName: String,
    /** One-sentence description shown on the setup card. */
    val purpose: String,
    /** Rough on-disk size for the friendly size label; mILM streams real bytes. */
    val approxBytes: Long,
)

object Models {

    val SMOLLM2 = ModelSpec(
        download = ModelDownload(
            id = "smollm2-360m",
            obj = "model",
            url = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf?download=true",
            ownedBy = "huggingface-tb",
        ),
        displayName = "Nudge writer",
        technicalName = "SmolLM2 360M · Q8_0",
        purpose = "Writes the short, personalized wellness suggestion you see each day.",
        approxBytes = 386_400_000L, // Q8_0 GGUF file, ~368 MiB
    )

    val QWEN3 = ModelSpec(
        download = ModelDownload(
            id = "qwen3-1.7b",
            obj = "model",
            url = "https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q8_0.gguf?download=true",
            ownedBy = "qwen",
        ),
        displayName = "Goal classifier",
        technicalName = "Qwen3 1.7B · Q8_0",
        purpose = "Reads ambiguous goals and groups your past nudges into Personal Tips.",
        approxBytes = 1_834_400_000L, // Q8_0 GGUF file, ~1.7 GiB
    )

    /** All models required before the main app is usable. */
    val ALL: List<ModelSpec> = listOf(SMOLLM2, QWEN3)
}
