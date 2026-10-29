package com.example.game.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.AiDifficulty
import com.example.game.model.FighterId

data class SavedGameProgress(
    val selectedFighter: FighterId = FighterId.IRON_BUFFALO,
    val highestStageCleared: Int = 0, // 0..3 (3 = Boss Defeated)
    val totalVictories: Int = 0,
    val bossesDefeated: Int = 0,
    val bestComboRecord: Int = 0,
    val totalPillarsShattered: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val cameraShakeEnabled: Boolean = true,
    val difficulty: AiDifficulty = AiDifficulty.WARRIOR
)

class LocalProgressRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadProgress(): SavedGameProgress {
        val fighterName = prefs.getString(KEY_SELECTED_FIGHTER, FighterId.IRON_BUFFALO.name)
            ?: FighterId.IRON_BUFFALO.name
        val fighterId = FighterId.entries.firstOrNull { it.name == fighterName && it != FighterId.SHADOW_DRAGON_OVERLORD }
            ?: FighterId.IRON_BUFFALO

        val diffName = prefs.getString(KEY_DIFFICULTY, AiDifficulty.WARRIOR.name)
            ?: AiDifficulty.WARRIOR.name
        val diff = AiDifficulty.entries.firstOrNull { it.name == diffName } ?: AiDifficulty.WARRIOR

        return SavedGameProgress(
            selectedFighter = fighterId,
            highestStageCleared = prefs.getInt(KEY_HIGHEST_STAGE, 0),
            totalVictories = prefs.getInt(KEY_TOTAL_VICTORIES, 0),
            bossesDefeated = prefs.getInt(KEY_BOSSES_DEFEATED, 0),
            bestComboRecord = prefs.getInt(KEY_BEST_COMBO, 0),
            totalPillarsShattered = prefs.getInt(KEY_PILLARS_SHATTERED, 0),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            cameraShakeEnabled = prefs.getBoolean(KEY_CAMERA_SHAKE, true),
            difficulty = diff
        )
    }

    fun saveSelectedFighter(fighterId: FighterId) {
        if (fighterId == FighterId.SHADOW_DRAGON_OVERLORD) return
        prefs.edit().putString(KEY_SELECTED_FIGHTER, fighterId.name).apply()
    }

    fun saveSettings(
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        cameraShakeEnabled: Boolean,
        difficulty: AiDifficulty
    ) {
        prefs.edit()
            .putBoolean(KEY_SOUND, soundEnabled)
            .putBoolean(KEY_HAPTICS, hapticsEnabled)
            .putBoolean(KEY_CAMERA_SHAKE, cameraShakeEnabled)
            .putString(KEY_DIFFICULTY, difficulty.name)
            .apply()
    }

    fun recordBattleVictory(
        stageNumber: Int,
        maxCombo: Int,
        pillarsBrokenInMatch: Int
    ): SavedGameProgress {
        val current = loadProgress()
        val newHighest = maxOf(current.highestStageCleared, stageNumber)
        val newVictories = current.totalVictories + 1
        val newBosses = current.bossesDefeated + if (stageNumber >= 3) 1 else 0
        val newBestCombo = maxOf(current.bestComboRecord, maxCombo)
        val newPillars = current.totalPillarsShattered + pillarsBrokenInMatch

        prefs.edit()
            .putInt(KEY_HIGHEST_STAGE, newHighest)
            .putInt(KEY_TOTAL_VICTORIES, newVictories)
            .putInt(KEY_BOSSES_DEFEATED, newBosses)
            .putInt(KEY_BEST_COMBO, newBestCombo)
            .putInt(KEY_PILLARS_SHATTERED, newPillars)
            .apply()

        return loadProgress()
    }

    fun resetProgress(): SavedGameProgress {
        val current = loadProgress()
        prefs.edit()
            .putInt(KEY_HIGHEST_STAGE, 0)
            .putInt(KEY_TOTAL_VICTORIES, 0)
            .putInt(KEY_BOSSES_DEFEATED, 0)
            .putInt(KEY_BEST_COMBO, 0)
            .putInt(KEY_PILLARS_SHATTERED, 0)
            .putBoolean(KEY_SOUND, current.soundEnabled)
            .putBoolean(KEY_HAPTICS, current.hapticsEnabled)
            .putBoolean(KEY_CAMERA_SHAKE, current.cameraShakeEnabled)
            .putString(KEY_DIFFICULTY, current.difficulty.name)
            .apply()
        return loadProgress()
    }

    companion object {
        private const val PREFS_NAME = "sn_tekken_offline_save"
        private const val KEY_SELECTED_FIGHTER = "selected_fighter"
        private const val KEY_HIGHEST_STAGE = "highest_stage"
        private const val KEY_TOTAL_VICTORIES = "total_victories"
        private const val KEY_BOSSES_DEFEATED = "bosses_defeated"
        private const val KEY_BEST_COMBO = "best_combo"
        private const val KEY_PILLARS_SHATTERED = "pillars_shattered"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_CAMERA_SHAKE = "camera_shake_enabled"
        private const val KEY_DIFFICULTY = "ai_difficulty"
    }
}
