package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.model.AppScreen
import com.example.game.model.FighterRoster
import com.example.game.ui.BattleArenaScreen
import com.example.game.ui.CampaignVictoryScreen
import com.example.game.ui.CharacterSelectScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.SettingsScreen
import com.example.game.viewmodel.GameViewModel
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immersive landscape arcade presentation while preserving safeDrawing insets on controls
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ArenaObsidian
                ) {
                    SnTekkenApp()
                }
            }
        }
    }
}

@Composable
fun SnTekkenApp(
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.uiState.collectAsStateWithLifecycle()
    val selectedProfile = FighterRoster.byId(state.selectedFighterId)

    when (state.currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                selectedFighter = selectedProfile,
                savedProgress = state.savedProgress,
                onPlayClicked = { gameViewModel.startNewArcadeCampaign() },
                onSelectFighterClicked = { gameViewModel.openCharacterSelect() },
                onSettingsClicked = { gameViewModel.openSettings() },
                onToggleSoundClicked = { gameViewModel.toggleSoundQuick() }
            )
        }

        AppScreen.CHARACTER_SELECT -> {
            CharacterSelectScreen(
                selectedFighter = selectedProfile,
                onSelectFighter = { gameViewModel.selectFighter(it) },
                onStartBattle = { gameViewModel.startNewArcadeCampaign() },
                onBack = { gameViewModel.returnToMainMenu() }
            )
        }

        AppScreen.BATTLE_ARENA -> {
            BattleArenaScreen(
                combatEngine = gameViewModel.combatEngine,
                hud = state.hud,
                onHudUpdated = { gameViewModel.onHudSnapshotFromRenderer(it) },
                onJoystickChanged = { moveX, jump -> gameViewModel.setJoystickInput(moveX, jump) },
                onBlockHeldChanged = { held -> gameViewModel.setBlockHeld(held) },
                onJumpPressed = { gameViewModel.triggerJump() },
                onDodgePressed = { gameViewModel.triggerDodge() },
                onMovePressed = { move -> gameViewModel.triggerMove(move) },
                onTogglePause = { gameViewModel.togglePause() },
                onNextBattle = { gameViewModel.advanceToNextBattle() },
                onRetryBattle = { gameViewModel.retryCurrentBattle() },
                onReturnToMenu = { gameViewModel.returnToMainMenu() }
            )
        }

        AppScreen.CAMPAIGN_VICTORY -> {
            CampaignVictoryScreen(
                championFighter = selectedProfile,
                totalDamage = state.campaignTotalDamage,
                maxCombo = state.campaignMaxCombo,
                arenaInteractions = state.campaignArenaInteractions,
                totalSeconds = state.campaignTotalSeconds,
                onRestartCampaign = { gameViewModel.startNewArcadeCampaign() },
                onReturnToMenu = { gameViewModel.returnToMainMenu() }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                savedProgress = state.savedProgress,
                onUpdateSettings = { sound, haptics, shake, diff ->
                    gameViewModel.updateSettings(sound, haptics, shake, diff)
                },
                onResetProgress = { gameViewModel.resetOfflineProgress() },
                onBack = { gameViewModel.returnToMainMenu() }
            )
        }
    }
}
