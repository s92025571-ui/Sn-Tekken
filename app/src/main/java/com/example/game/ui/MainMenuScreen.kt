package com.example.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.FighterProfile
import com.example.game.storage.SavedGameProgress
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.UltimateGold

@Composable
fun MainMenuScreen(
    selectedFighter: FighterProfile,
    savedProgress: SavedGameProgress,
    onPlayClicked: () -> Unit,
    onSelectFighterClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onToggleSoundClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .testTag("main_menu_screen")
    ) {
        // Generated dark cinematic Beast Battle Arena hero banner background
        Image(
            painter = painterResource(id = R.drawable.img_arena_hero_banner_1793265095032),
            contentDescription = stringResource(R.string.arena_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Atmospheric cybernetic dark gradient vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ArenaObsidian.copy(alpha = 0.93f),
                            ArenaObsidian.copy(alpha = 0.72f),
                            ArenaObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Title, Offline/Free Guarantee Badge, & Primary Navigation Buttons
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.14f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.badge_offline_free),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.subtitle_animal_warriors),
                    style = MaterialTheme.typography.headlineMedium,
                    color = BlazingOrange
                )

                Spacer(Modifier.height(14.dp))

                // Primary Play Arcade Campaign CTA
                Button(
                    onClick = onPlayClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(52.dp)
                        .testTag("play_arcade_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.menu_play_arcade),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSelectFighterClicked,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("character_select_button")
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.menu_select_fighter),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onSettingsClicked,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.menu_settings),
                            tint = NeonCyan
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleSoundClicked,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (savedProgress.soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = stringResource(
                                if (savedProgress.soundEnabled) R.string.menu_sound_on else R.string.menu_sound_off
                            ),
                            tint = if (savedProgress.soundEnabled) UltimateGold else Color.Gray
                        )
                    }
                }
            }

            // Right Column: Live 3D Warrior Preview Card + Local Arcade Progress Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = ArenaSurface.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .border(1.5.dp, selectedFighter.accentColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE WARRIOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Text(
                                text = selectedFighter.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = selectedFighter.accentColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = UltimateGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${savedProgress.totalVictories} WINS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = UltimateGold
                                )
                            }
                            Text(
                                text = "Bosses Slain: ${savedProgress.bossesDefeated} • Best Combo: ${savedProgress.bestComboRecord}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live OpenGL 3D Turntable of selected Animal Warrior
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    ) {
                        GlArenaSurfaceView(
                            combatEngine = null,
                            previewFighterProvider = { selectedFighter },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "ULTIMATE: ${selectedFighter.ultimateName} • ARENA: BEAST BATTLE ARENA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
