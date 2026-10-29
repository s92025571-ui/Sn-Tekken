package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.game.audio.ProceduralArcadeSynthesizer
import com.example.game.audio.SoundCue
import com.example.game.engine.CombatEngine
import com.example.game.model.AiDifficulty
import com.example.game.model.AppScreen
import com.example.game.model.CombatHudSnapshot
import com.example.game.model.FighterId
import com.example.game.model.FighterProfile
import com.example.game.model.FighterRoster
import com.example.game.model.MoveType
import com.example.game.storage.LocalProgressRepository
import com.example.game.storage.SavedGameProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SnTekkenUiState(
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val selectedFighterId: FighterId = FighterId.IRON_BUFFALO,
    val campaignStage: Int = 1, // 1, 2, or 3 (Boss)
    val savedProgress: SavedGameProgress = SavedGameProgress(),
    val hud: CombatHudSnapshot = CombatHudSnapshot(),
    val campaignTotalDamage: Int = 0,
    val campaignMaxCombo: Int = 0,
    val campaignArenaInteractions: Int = 0,
    val campaignTotalSeconds: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepo = LocalProgressRepository(application)
    val synthesizer = ProceduralArcadeSynthesizer(application)
    val combatEngine = CombatEngine(synthesizer)

    private val _uiState = MutableStateFlow(SnTekkenUiState())
    val uiState: StateFlow<SnTekkenUiState> = _uiState.asStateFlow()

    private var victoryRecordedForCurrentStage = false

    init {
        val loaded = progressRepo.loadProgress()
        synthesizer.soundEnabled = loaded.soundEnabled
        synthesizer.hapticsEnabled = loaded.hapticsEnabled
        combatEngine.cameraShakeEnabled = loaded.cameraShakeEnabled
        _uiState.value = SnTekkenUiState(
            currentScreen = AppScreen.MAIN_MENU,
            selectedFighterId = loaded.selectedFighter,
            savedProgress = loaded
        )
    }

    val selectedFighterProfile: FighterProfile
        get() = FighterRoster.byId(_uiState.value.selectedFighterId)

    fun openCharacterSelect() {
        synthesizer.play(SoundCue.MENU_CLICK)
        _uiState.update { it.copy(currentScreen = AppScreen.CHARACTER_SELECT) }
    }

    fun openSettings() {
        synthesizer.play(SoundCue.MENU_CLICK)
        _uiState.update { it.copy(currentScreen = AppScreen.SETTINGS) }
    }

    fun returnToMainMenu() {
        synthesizer.play(SoundCue.MENU_CLICK)
        combatEngine.isPaused = true
        _uiState.update { it.copy(currentScreen = AppScreen.MAIN_MENU) }
    }

    fun selectFighter(fighterId: FighterId) {
        if (fighterId == FighterId.SHADOW_DRAGON_OVERLORD) return
        synthesizer.play(SoundCue.MENU_CLICK)
        progressRepo.saveSelectedFighter(fighterId)
        val refreshed = progressRepo.loadProgress()
        _uiState.update {
            it.copy(
                selectedFighterId = fighterId,
                savedProgress = refreshed
            )
        }
    }

    fun toggleSoundQuick() {
        val cur = _uiState.value.savedProgress
        updateSettings(
            soundEnabled = !cur.soundEnabled,
            hapticsEnabled = cur.hapticsEnabled,
            cameraShakeEnabled = cur.cameraShakeEnabled,
            difficulty = cur.difficulty
        )
    }

    fun updateSettings(
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        cameraShakeEnabled: Boolean,
        difficulty: AiDifficulty
    ) {
        progressRepo.saveSettings(soundEnabled, hapticsEnabled, cameraShakeEnabled, difficulty)
        synthesizer.soundEnabled = soundEnabled
        synthesizer.hapticsEnabled = hapticsEnabled
        combatEngine.cameraShakeEnabled = cameraShakeEnabled
        if (soundEnabled) synthesizer.play(SoundCue.MENU_CLICK)
        val refreshed = progressRepo.loadProgress()
        _uiState.update { it.copy(savedProgress = refreshed) }
    }

    fun resetOfflineProgress() {
        synthesizer.play(SoundCue.MENU_CLICK)
        val reset = progressRepo.resetProgress()
        _uiState.update { it.copy(savedProgress = reset) }
    }

    fun startNewArcadeCampaign() {
        val state = _uiState.value
        val playerFighter = FighterRoster.byId(state.selectedFighterId)
        val opponents = FighterRoster.buildCampaignOpponents(state.selectedFighterId)
        val stage1Opponent = opponents[0]

        victoryRecordedForCurrentStage = false
        combatEngine.startBattle(
            playerProfile = playerFighter,
            opponentProfile = stage1Opponent,
            stage = 1,
            aiDiff = state.savedProgress.difficulty
        )
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.BATTLE_ARENA,
                campaignStage = 1,
                campaignTotalDamage = 0,
                campaignMaxCombo = 0,
                campaignArenaInteractions = 0,
                campaignTotalSeconds = 0
            )
        }
    }

    fun advanceToNextBattle() {
        val state = _uiState.value
        val nextStage = state.campaignStage + 1
        if (nextStage > 3) {
            synthesizer.play(SoundCue.VICTORY_FANFARE)
            _uiState.update { it.copy(currentScreen = AppScreen.CAMPAIGN_VICTORY) }
            return
        }

        val playerFighter = FighterRoster.byId(state.selectedFighterId)
        val opponents = FighterRoster.buildCampaignOpponents(state.selectedFighterId)
        val nextOpponent = opponents[(nextStage - 1).coerceIn(0, 2)]

        victoryRecordedForCurrentStage = false
        combatEngine.startBattle(
            playerProfile = playerFighter,
            opponentProfile = nextOpponent,
            stage = nextStage,
            aiDiff = state.savedProgress.difficulty
        )
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.BATTLE_ARENA,
                campaignStage = nextStage
            )
        }
    }

    fun retryCurrentBattle() {
        val state = _uiState.value
        val stage = state.campaignStage.coerceIn(1, 3)
        val playerFighter = FighterRoster.byId(state.selectedFighterId)
        val opponents = FighterRoster.buildCampaignOpponents(state.selectedFighterId)
        val opponent = opponents[stage - 1]

        victoryRecordedForCurrentStage = false
        combatEngine.startBattle(
            playerProfile = playerFighter,
            opponentProfile = opponent,
            stage = stage,
            aiDiff = state.savedProgress.difficulty
        )
    }

    fun onHudSnapshotFromRenderer(snapshot: CombatHudSnapshot) {
        if (snapshot.isBattleOver && snapshot.didPlayerWin && !victoryRecordedForCurrentStage) {
            victoryRecordedForCurrentStage = true
            val updatedSave = progressRepo.recordBattleVictory(
                stageNumber = snapshot.stageNumber,
                maxCombo = snapshot.maxComboAchieved,
                pillarsBrokenInMatch = snapshot.brokenPillarsCount
            )
            _uiState.update { prev ->
                prev.copy(
                    hud = snapshot,
                    savedProgress = updatedSave,
                    campaignTotalDamage = prev.campaignTotalDamage + snapshot.damageDealtTotal,
                    campaignMaxCombo = maxOf(prev.campaignMaxCombo, snapshot.maxComboAchieved),
                    campaignArenaInteractions = prev.campaignArenaInteractions + snapshot.arenaInteractionsTriggered,
                    campaignTotalSeconds = prev.campaignTotalSeconds + snapshot.battleElapsedSeconds
                )
            }
        } else {
            _uiState.update { it.copy(hud = snapshot) }
        }
    }

    // Touch controls bridged directly to CombatEngine
    fun setJoystickInput(moveX: Float, jumpTriggered: Boolean) {
        combatEngine.inputMoveX = moveX.coerceIn(-1f, 1f)
        if (jumpTriggered) {
            combatEngine.requestPlayerJump()
        }
    }

    fun setBlockHeld(held: Boolean) {
        combatEngine.inputBlockHeld = held
    }

    fun triggerJump() {
        combatEngine.requestPlayerJump()
    }

    fun triggerDodge() {
        combatEngine.requestPlayerDodge()
    }

    fun triggerMove(moveType: MoveType) {
        combatEngine.requestPlayerAttack(moveType)
    }

    fun togglePause() {
        synthesizer.play(SoundCue.MENU_CLICK)
        combatEngine.isPaused = !combatEngine.isPaused
    }
}
