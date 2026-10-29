package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.engine.CombatEngine
import com.example.game.model.CombatHudSnapshot
import com.example.game.model.MoveType
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OverlordCrimson
import com.example.ui.theme.UltimateGold
import kotlin.math.hypot

@Composable
fun BattleArenaScreen(
    combatEngine: CombatEngine,
    hud: CombatHudSnapshot,
    onHudUpdated: (CombatHudSnapshot) -> Unit,
    onJoystickChanged: (Float, Boolean) -> Unit,
    onBlockHeldChanged: (Boolean) -> Unit,
    onJumpPressed: () -> Unit,
    onDodgePressed: () -> Unit,
    onMovePressed: (MoveType) -> Unit,
    onTogglePause: () -> Unit,
    onNextBattle: () -> Unit,
    onRetryBattle: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    BackHandler {
        if (!hud.isBattleOver) {
            onTogglePause()
        } else {
            onReturnToMenu()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .testTag("battle_arena_screen")
    ) {
        // 1. Live 60fps OpenGL ES 3D Viewport
        GlArenaSurfaceView(
            combatEngine = combatEngine,
            onHudUpdated = onHudUpdated,
            modifier = Modifier
                .fillMaxSize()
                .testTag("gl_arena_viewport")
        )

        // 2. Safe-area Combat HUD & Responsive Multi-Touch Arcade Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD: Health Bars, Timer, Stage, Ultimate Meter, Pillar Status
            TopCombatHud(
                hud = hud,
                onTogglePause = onTogglePause
            )

            // CENTER BANNERS: Intro/KO Banner, Boss Lethal Telegraph Warning, Environmental Tactical Status
            CenterCombatAlerts(hud = hud)

            // BOTTOM CONTROLS: Virtual Joystick (Left) + Arcade 7-Button Cluster (Right)
            if (!hud.isBattleOver && !hud.isPaused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Side: Virtual Movement Joystick + Dedicated Jump Trigger
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VirtualArcadeJoystick(
                            onJoystickChanged = onJoystickChanged,
                            modifier = Modifier.testTag("virtual_joystick")
                        )

                        ArcadeActionPadButton(
                            label = stringResource(R.string.ctrl_jump),
                            accentColor = NeonCyan,
                            size = 52.dp,
                            onTap = onJumpPressed,
                            modifier = Modifier.testTag("btn_jump")
                        )
                    }

                    // Right Side: Punch, Kick, Heavy, Block, Dodge, Special, Ultimate
                    RightSideCombatCluster(
                        ultimateReady = hud.playerUltimate >= 100f,
                        ultimateProgress = hud.playerUltimate / 100f,
                        onBlockHeldChanged = onBlockHeldChanged,
                        onDodgePressed = onDodgePressed,
                        onMovePressed = onMovePressed
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }

        // 3. Pause Modal Overlay
        if (hud.isPaused && !hud.isBattleOver) {
            PauseOverlayDialog(
                onResume = onTogglePause,
                onRestart = onRetryBattle,
                onReturnMenu = onReturnToMenu
            )
        }

        // 4. End-of-Battle Result Overlay (Next Battle / Boss Victory / Retry / Menu)
        if (hud.isBattleOver) {
            BattleResultOverlay(
                hud = hud,
                onNextBattle = onNextBattle,
                onRetryBattle = onRetryBattle,
                onReturnMenu = onReturnToMenu
            )
        }
    }
}

@Composable
private fun TopCombatHud(
    hud: CombatHudSnapshot,
    onTogglePause: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Health + Ultimate Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = hud.playerProfile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = hud.playerProfile.accentColor
                    )
                    Text(
                        text = "${hud.playerHp.toInt()}/${hud.playerMaxHp.toInt()} HP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { (hud.playerHp / hud.playerMaxHp.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                    color = if (hud.playerHp > 30f) HealthEmerald else NeonCrimson,
                    trackColor = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .testTag("player_health_bar")
                )
                Spacer(Modifier.height(4.dp))
                // Ultimate Energy Meter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hud.playerUltimate >= 100f) "ULTIMATE MAX!" else "ULT ${hud.playerUltimate.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = UltimateGold,
                        modifier = Modifier.width(86.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (hud.playerUltimate / 100f).coerceIn(0f, 1f) },
                        color = UltimateGold,
                        trackColor = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("player_ultimate_bar")
                    )
                }
                if (hud.playerCombo >= 2) {
                    Text(
                        text = "${hud.playerCombo} HIT COMBO!",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                }
            }

            // Center Stage & Round Timer + Pause Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hud.opponentProfile.isBoss) "BOSS STAGE" else "STAGE ${hud.stageNumber}/3",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArenaSurface.copy(alpha = 0.9f))
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "%02d".format(hud.roundTimerSeconds),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                }
                Text(
                    text = "PILLARS: ${hud.totalPillarsCount - hud.brokenPillarsCount}/${hud.totalPillarsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Opponent / Boss Health Bar
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${hud.opponentHp.toInt()}/${hud.opponentMaxHp.toInt()} HP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = if (hud.opponentProfile.isBoss && hud.bossPhase == 2) {
                            "${hud.opponentProfile.displayName} [PHASE 2]"
                        } else {
                            hud.opponentProfile.displayName
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = hud.opponentProfile.accentColor
                    )
                }
                LinearProgressIndicator(
                    progress = { (hud.opponentHp / hud.opponentMaxHp.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                    color = if (hud.opponentProfile.isBoss) OverlordCrimson else BlazingOrange,
                    trackColor = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (hud.opponentProfile.isBoss) 16.dp else 13.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = if (hud.opponentProfile.isBoss) 1.5.dp else 1.dp,
                            color = if (hud.opponentProfile.isBoss) UltimateGold else Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .testTag("opponent_health_bar")
                )
                if (hud.opponentCombo >= 2) {
                    Text(
                        text = "${hud.opponentCombo} HIT COMBO",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCrimson
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterCombatAlerts(hud: CombatHudSnapshot) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimatedVisibility(visible = hud.bossWarningText.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OverlordCrimson.copy(alpha = 0.88f))
                    .border(1.5.dp, UltimateGold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = UltimateGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = hud.bossWarningText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(visible = hud.bannerAnnouncement.isNotEmpty() && !hud.isBattleOver) {
            Text(
                text = hud.bannerAnnouncement,
                style = MaterialTheme.typography.displayMedium,
                color = UltimateGold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ArenaObsidian.copy(alpha = 0.72f))
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        AnimatedVisibility(visible = hud.activeTacticalStatus.isNotEmpty()) {
            Text(
                text = hud.activeTacticalStatus,
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ArenaObsidian.copy(alpha = 0.75f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun VirtualArcadeJoystick(
    onJoystickChanged: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    var jumpFiredInDrag by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(124.dp)
            .pointerInput(Unit) {
                val radiusPx = size.width * 0.42f
                detectDragGestures(
                    onDragStart = { offset ->
                        val dx = offset.x - size.width / 2f
                        val dy = offset.y - size.height / 2f
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > radiusPx) radiusPx / dist else 1f
                        knobX = (dx * clamped) / radiusPx
                        knobY = (dy * clamped) / radiusPx
                        val jumpNow = knobY < -0.68f && !jumpFiredInDrag
                        if (jumpNow) jumpFiredInDrag = true
                        onJoystickChanged(knobX, jumpNow)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val dx = change.position.x - size.width / 2f
                        val dy = change.position.y - size.height / 2f
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > radiusPx) radiusPx / dist else 1f
                        knobX = (dx * clamped) / radiusPx
                        knobY = (dy * clamped) / radiusPx
                        val jumpNow = knobY < -0.68f && !jumpFiredInDrag
                        if (jumpNow) jumpFiredInDrag = true
                        if (knobY > -0.35f) jumpFiredInDrag = false
                        onJoystickChanged(knobX, jumpNow)
                    },
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        jumpFiredInDrag = false
                        onJoystickChanged(0f, false)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        jumpFiredInDrag = false
                        onJoystickChanged(0f, false)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.44f
            drawCircle(
                color = Color(0xAA0D1322),
                radius = baseRadius,
                center = center
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.65f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 4f)
            )
            val stickCenter = Offset(
                x = center.x + knobX * baseRadius * 0.62f,
                y = center.y + knobY * baseRadius * 0.62f
            )
            drawCircle(
                color = NeonCyan,
                radius = baseRadius * 0.36f,
                center = stickCenter
            )
        }
    }
}

@Composable
private fun RightSideCombatCluster(
    ultimateReady: Boolean,
    ultimateProgress: Float,
    onBlockHeldChanged: (Boolean) -> Unit,
    onDodgePressed: () -> Unit,
    onMovePressed: (MoveType) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Upper Row: ULTIMATE (glows gold when full), SPECIAL (Melee/Fire/Rush), PROJECTILE, BLOCK (Hold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcadeActionPadButton(
                label = if (ultimateReady) "ULTIMATE!" else "ULT ${(ultimateProgress * 100).toInt()}%",
                accentColor = if (ultimateReady) UltimateGold else Color.Gray,
                filled = ultimateReady,
                size = 62.dp,
                onTap = { if (ultimateReady) onMovePressed(MoveType.ULTIMATE) },
                modifier = Modifier.testTag("btn_ultimate")
            )

            ArcadeActionPadButton(
                label = stringResource(R.string.ctrl_special),
                accentColor = BlazingOrange,
                size = 56.dp,
                onTap = { onMovePressed(MoveType.SPECIAL_MELEE) },
                modifier = Modifier.testTag("btn_special")
            )

            ArcadeActionPadButton(
                label = "BLAST",
                accentColor = NeonCyan,
                size = 54.dp,
                onTap = { onMovePressed(MoveType.PROJECTILE) },
                modifier = Modifier.testTag("btn_projectile")
            )

            // Hold-to-Block tactile button
            HoldableBlockButton(
                onBlockHeldChanged = onBlockHeldChanged,
                modifier = Modifier.testTag("btn_block")
            )
        }

        // Lower Primary Row: DODGE, PUNCH, KICK, HEAVY (Floor Shatter / Pillar Breaker)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcadeActionPadButton(
                label = stringResource(R.string.ctrl_dodge),
                accentColor = NeonCyan,
                size = 54.dp,
                onTap = onDodgePressed,
                modifier = Modifier.testTag("btn_dodge")
            )

            ArcadeActionPadButton(
                label = stringResource(R.string.ctrl_punch),
                accentColor = Color.White,
                size = 60.dp,
                onTap = { onMovePressed(MoveType.PUNCH) },
                modifier = Modifier.testTag("btn_punch")
            )

            ArcadeActionPadButton(
                label = stringResource(R.string.ctrl_kick),
                accentColor = BlazingOrange,
                size = 60.dp,
                onTap = { onMovePressed(MoveType.KICK) },
                modifier = Modifier.testTag("btn_kick")
            )

            ArcadeActionPadButton(
                label = stringResource(R.string.ctrl_heavy),
                accentColor = NeonCrimson,
                filled = true,
                size = 64.dp,
                onTap = { onMovePressed(MoveType.HEAVY) },
                modifier = Modifier.testTag("btn_heavy")
            )
        }
    }
}

@Composable
private fun ArcadeActionPadButton(
    label: String,
    accentColor: Color,
    size: Dp,
    filled: Boolean = false,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                when {
                    pressed -> accentColor.copy(alpha = 0.85f)
                    filled -> accentColor.copy(alpha = 0.36f)
                    else -> ArenaObsidian.copy(alpha = 0.78f)
                }
            )
            .border(2.dp, accentColor, CircleShape)
            .pointerInput(onTap) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTap()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (pressed) ArenaObsidian else Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HoldableBlockButton(
    onBlockHeldChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var held by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (held) NeonCyan.copy(alpha = 0.75f) else ArenaObsidian.copy(alpha = 0.8f))
            .border(2.dp, NeonCyan, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held = true
                        onBlockHeldChanged(true)
                        tryAwaitRelease()
                        held = false
                        onBlockHeldChanged(false)
                    }
                )
            }
    ) {
        Text(
            text = stringResource(R.string.ctrl_block),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (held) ArenaObsidian else NeonCyan
        )
    }
}

@Composable
private fun PauseOverlayDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onReturnMenu: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian.copy(alpha = 0.82f))
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(340.dp)
                .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "BATTLE PAUSED",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("RESUME FIGHT", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("RESTART STAGE", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onReturnMenu,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.btn_return_menu), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun BattleResultOverlay(
    hud: CombatHudSnapshot,
    onNextBattle: () -> Unit,
    onRetryBattle: () -> Unit,
    onReturnMenu: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaObsidian.copy(alpha = 0.78f))
            .testTag("battle_result_overlay")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(420.dp)
                .border(
                    width = 2.dp,
                    color = if (hud.didPlayerWin) UltimateGold else NeonCrimson,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = if (hud.didPlayerWin) {
                        if (hud.stageNumber >= 3) "SHADOW OVERLORD DEFEATED!" else "STAGE ${hud.stageNumber} VICTORY!"
                    } else {
                        "WARRIOR FALLEN"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (hud.didPlayerWin) UltimateGold else NeonCrimson
                )

                Text(
                    text = "Damage Dealt: ${hud.damageDealtTotal} • Max Combo: ${hud.maxComboAchieved}x • Arena Interactions: ${hud.arenaInteractionsTriggered}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hud.didPlayerWin) {
                        Button(
                            onClick = onNextBattle,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_battle_button")
                        ) {
                            Text(
                                text = if (hud.stageNumber >= 3) "CHAMPION SUMMARY" else stringResource(R.string.btn_next_battle),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = onRetryBattle,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("retry_battle_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_retry_battle),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onReturnMenu,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("return_menu_button")
                    ) {
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
