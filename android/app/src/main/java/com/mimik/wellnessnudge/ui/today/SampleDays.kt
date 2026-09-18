package com.mimik.wellnessnudge.ui.today

import android.content.Context
import com.mimik.wellnessnudge.demo.MockNudgeInput
import com.mimik.wellnessnudge.demo.MockNudgeInputs

/**
 * The scenarios behind "Sample day", taken round-robin from [MockNudgeInputs]. The position
 * is kept in shared preferences, so every tap brings the next day, even across restarts.
 */
internal class SampleDays(context: Context) {

    // Opened on first use, so building the screen never touches the disk.
    private val prefs by lazy { context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE) }

    fun next(): MockNudgeInput {
        val all = MockNudgeInputs.ALL
        val index = prefs.getInt(KeyIndex, 0).coerceIn(0, all.lastIndex)
        prefs.edit().putInt(KeyIndex, (index + 1) % all.size).apply()
        return all[index]
    }

    private companion object {
        const val PrefsName = "wellness_demo"
        const val KeyIndex = "mock_idx"
    }
}
