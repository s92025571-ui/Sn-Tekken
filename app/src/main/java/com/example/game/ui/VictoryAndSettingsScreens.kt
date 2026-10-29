package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.AiDifficulty
import com.example.game.model.FighterProfile
import com.example.game.storage.SavedGameProgress
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.UltimateGold

@Composable
fun CampaignVictoryScreen(
    championFighter: FighterProfile,
    totalDamage: Int,
    maxCombo: Int,
    arenaInteractions: Int,
    totalSeconds: Int,
    onRestartCampaign: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    BackHandler { onReturnToMenu() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .testTag("campaign_victory_screen"),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Live 3D Champion Pedestal
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, UltimateGold, RoundedCornerShape(16.dp))
        ) {
            GlArenaSurfaceView(
                combatEngine = null,
                previewFighterProvider = { championFighter },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "BEAST ARENA GRAND CHAMPION",
                style = MaterialTheme.typography.headlineMedium,
                color = UltimateGold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArenaObsidian.copy(alpha = 0.8f))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        // Right: Battle Statistics & Replay / Menu Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = UltimateGold,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SHADOW OVERLORD CONQUERED!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = UltimateGold
                        )
                    }
                    Text(
                        text = "${championFighter.displayName} triumphed across all 3 battles in the Beast Battle Arena.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArenaSurfaceVariant)
                        .padding(14.dp)
                ) {
                    VictoryStatRow("Champion Warrior", championFighter.displayName)
                    VictoryStatRow("Total Campaign Damage", "$totalDamage DMG")
                    VictoryStatRow("Peak Combo Chain", "${maxCombo}x HITS")
                    VictoryStatRow("Arena Tactical Interactions", "$arenaInteractions Triggered")
                    VictoryStatRow("Total Combat Time", "${totalSeconds}s")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRestartCampaign,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("victory_restart_button")
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_restart_campaign),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = onReturnToMenu,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("victory_menu_button")
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_return_menu),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
    }
}

@Composable
fun SettingsScreen(
    savedProgress: SavedGameProgress,
    onUpdateSettings: (Boolean, Boolean, Boolean, AiDifficulty) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("settings_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = NeonCyan
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }

        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingToggleRow(
                    title = "Procedural Arcade Sound Effects",
                    subtitle = "100% offline synthesized martial arts impacts, roars, and pillar shatter audio",
                    checked = savedProgress.soundEnabled,
                    onCheckedChange = {
                        onUpdateSettings(it, savedProgress.hapticsEnabled, savedProgress.cameraShakeEnabled, savedProgress.difficulty)
                    }
                )

                SettingToggleRow(
                    title = stringResource(R.string.settings_haptics),
                    subtitle = "Tactile vibration feedback on heavy blows, blocks, and Ultimate finishers",
                    checked = savedProgress.hapticsEnabled,
                    onCheckedChange = {
                        onUpdateSettings(savedProgress.soundEnabled, it, savedProgress.cameraShakeEnabled, savedProgress.difficulty)
                    }
                )

                SettingToggleRow(
                    title = stringResource(R.string.settings_camera_shake),
                    subtitle = "Dynamic screen shake on floor stomps, pillar collapses, and Ultimate attacks",
                    checked = savedProgress.cameraShakeEnabled,
                    onCheckedChange = {
                        onUpdateSettings(savedProgress.soundEnabled, savedProgress.hapticsEnabled, it, savedProgress.difficulty)
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_difficulty),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AiDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = savedProgress.difficulty == diff,
                                onClick = {
                                    onUpdateSettings(
                                        savedProgress.soundEnabled,
                                        savedProgress.hapticsEnabled,
                                        savedProgress.cameraShakeEnabled,
                                        diff
                                    )
                                },
                                label = {
                                    Text(diff.label, style = MaterialTheme.typography.titleMedium)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Offline Save Records",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan
                        )
                        Text(
                            text = "Total Wins: ${savedProgress.totalVictories} • Bosses Defeated: ${savedProgress.bossesDefeated} • Pillars Shattered: ${savedProgress.totalPillarsShattered}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onResetProgress,
                        modifier = Modifier.height(48.dp).testTag("reset_progress_button")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = NeonCrimson)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_reset_progress), color = NeonCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
