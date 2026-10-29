package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.model.AiDifficulty
import com.example.game.model.FighterId
import com.example.game.storage.LocalProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `app name matches SN TEKKEN and offline progress saves locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SN TEKKEN", appName)

        val repo = LocalProgressRepository(context)
        repo.resetProgress()
        repo.saveSelectedFighter(FighterId.SHADOW_TIGER)
        repo.saveSettings(
            soundEnabled = false,
            hapticsEnabled = true,
            cameraShakeEnabled = true,
            difficulty = AiDifficulty.OVERLORD
        )
        val afterWin = repo.recordBattleVictory(stageNumber = 3, maxCombo = 12, pillarsBrokenInMatch = 3)

        assertEquals(FighterId.SHADOW_TIGER, afterWin.selectedFighter)
        assertEquals(3, afterWin.highestStageCleared)
        assertEquals(1, afterWin.bossesDefeated)
        assertEquals(12, afterWin.bestComboRecord)
        assertEquals(3, afterWin.totalPillarsShattered)
        assertEquals(false, afterWin.soundEnabled)
    }
}
