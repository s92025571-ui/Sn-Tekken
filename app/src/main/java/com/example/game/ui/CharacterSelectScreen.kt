package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.FighterId
import com.example.game.model.FighterProfile
import com.example.game.model.FighterRoster
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.UltimateGold

@Composable
fun CharacterSelectScreen(
    selectedFighter: FighterProfile,
    onSelectFighter: (FighterId) -> Unit,
    onStartBattle: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val currentPreview by rememberUpdatedState(selectedFighter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("character_select_screen")
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("char_select_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back),
                        tint = NeonCyan
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "CHOOSE YOUR ANIMAL WARRIOR",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }

            Button(
                onClick = onStartBattle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("start_battle_button")
            ) {
                Icon(Icons.Filled.SportsMma, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_start_battle),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 3-Column Landscape Canonical Layout: Roster List | Live 3D Animated Warrior | Stats, Abilities & Arena Synergy
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column: 4 Playable Animal Warriors
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FighterRoster.playableFighters.forEach { fighter ->
                    val isSelected = fighter.id == selectedFighter.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ArenaSurfaceVariant else ArenaSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) fighter.accentColor else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFighter(fighter.id) }
                            .testTag("fighter_card_${fighter.id.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = fighter.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isSelected) fighter.accentColor else Color.White
                            )
                            Text(
                                text = "${fighter.animalIdentity} • ${fighter.fightingStyle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Center Column: Real-Time 3D Animated Model Viewport
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, selectedFighter.accentColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .testTag("fighter_3d_preview")
            ) {
                GlArenaSurfaceView(
                    combatEngine = null,
                    previewFighterProvider = { currentPreview },
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(ArenaObsidian.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = selectedFighter.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = selectedFighter.accentColor
                    )
                    Text(
                        text = selectedFighter.fightingStyle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            // Right Column: Stats, Ultimate Power, Environmental Interaction & Signature Abilities
            Card(
                colors = CardDefaults.cardColors(containerColor = ArenaSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "COMBAT ATTRIBUTES",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )

                    StatRatingBar("POWER", selectedFighter.statPowerRating, selectedFighter.accentColor)
                    StatRatingBar("SPEED", selectedFighter.statSpeedRating, NeonCyan)
                    StatRatingBar("DEFENSE", selectedFighter.statDefenseRating, UltimateGold)
                    StatRatingBar("REACH / PROJ", selectedFighter.statRangeRating, NeonCrimson)

                    // Ultimate Power Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ArenaSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = UltimateGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ULTIMATE: ${selectedFighter.ultimateName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = UltimateGold
                            )
                        }
                        Text(
                            text = selectedFighter.ultimateDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    // Environmental Arena Interaction Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.10f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Terrain,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ARENA INTERACTION TACTIC",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = selectedFighter.arenaTacticalAdvantage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "SIGNATURE MOVESET",
                        style = MaterialTheme.typography.labelLarge,
                        color = selectedFighter.accentColor
                    )

                    selectedFighter.abilities.forEach { ability ->
                        Column {
                            Text(
                                text = "• ${ability.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = ability.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRatingBar(label: String, rating: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        LinearProgressIndicator(
            progress = { (rating / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$rating",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.width(24.dp)
        )
    }
}
