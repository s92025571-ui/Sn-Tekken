package com.example.game.engine

import com.example.game.audio.ProceduralArcadeSynthesizer
import com.example.game.audio.SoundCue
import com.example.game.model.ActionState
import com.example.game.model.AiDifficulty
import com.example.game.model.ArenaHazardType
import com.example.game.model.ArenaPillar
import com.example.game.model.ArenaZoneEffect
import com.example.game.model.CombatHudSnapshot
import com.example.game.model.FighterId
import com.example.game.model.FighterProfile
import com.example.game.model.MoveType
import com.example.game.model.Particle3D
import com.example.game.model.ProjectileEntity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FighterActor(
    val profile: FighterProfile,
    val isPlayer: Boolean,
    var x: Float,
    var facingSign: Float // +1f faces right (+X), -1f faces left (-X)
) {
    var y: Float = 0f
    var z: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f

    var hp: Float = profile.maxHealth
    var ultimateEnergy: Float = 0f

    var state: ActionState = ActionState.INTRO
    var stateTimer: Float = 0f
    var animTime: Float = 0f

    var currentMove: MoveType = MoveType.NONE
    var moveDuration: Float = 0f
    var moveHitApplied: Boolean = false
    var ultimateSubHitsRemaining: Int = 0
    var ultimateNextHitTime: Float = 0f

    var comboCount: Int = 0
    var comboDecayTimer: Float = 0f
    var hitFlashTimer: Float = 0f
    var specialGlowTimer: Float = 0f
    var stealthSmokeCloakTimer: Float = 0f
    var slowedByFloorCrackTimer: Float = 0f

    // Cooldowns to prevent spamming
    var lightCooldown: Float = 0f
    var heavyCooldown: Float = 0f
    var specialCooldown: Float = 0f
    var projectileCooldown: Float = 0f
    var dodgeCooldown: Float = 0f

    val isGrounded: Boolean get() = y <= 0.02f
    val isInvulnerableDodge: Boolean get() = state == ActionState.DODGE && stateTimer < 0.28f
    val isBlockingActive: Boolean get() = state == ActionState.BLOCK && isGrounded
}

class CombatEngine(
    private val audio: ProceduralArcadeSynthesizer? = null
) {
    private val lock = Any()
    private val rng = Random(42)

    val arenaHalfWidth = 7.4f
    private val gravity = -22.5f

    lateinit var player: FighterActor
        private set
    lateinit var opponent: FighterActor
        private set

    var stageNumber: Int = 1
        private set
    var difficulty: AiDifficulty = AiDifficulty.WARRIOR
    var cameraShakeEnabled: Boolean = true

    val pillars = mutableListOf<ArenaPillar>()
    val zoneEffects = mutableListOf<ArenaZoneEffect>()
    val projectiles = mutableListOf<ProjectileEntity>()
    val particles = mutableListOf<Particle3D>()

    private var nextEntityId = 1
    private var introTimer = 2.1f
    private var roundTimer = 99f
    private var elapsedMatchTime = 0f
    private var bannerText = "STAGE 1 — BEAST BATTLE ARENA"
    private var bannerTimer = 2.1f
    private var bossWarningText = ""
    private var bossWarningTimer = 0f
    private var bossPhase = 1
    private var bossPendingMajorMove: MoveType = MoveType.NONE
    private var bossTelegraphTimer = 0f

    private var aiDecisionTimer = 0.35f
    private var aiDesiredMoveDir = 0f
    private var aiHoldBlockTimer = 0f

    // Continuous touch input state from UI
    @Volatile
    var inputMoveX: Float = 0f

    @Volatile
    var inputBlockHeld: Boolean = false

    @Volatile
    var isPaused: Boolean = false

    // Camera & juice telemetry read by 3D renderer
    var cameraZoomUltimate: Float = 0f
        private set
    var cameraShakeIntensity: Float = 0f
        private set
    var arenaFlashIntensity: Float = 0f
        private set
    var arenaFlashColorHex: Int = 0xFF00E5FF.toInt()
        private set

    // Match statistics
    private var totalDamageDealt: Float = 0f
    private var maxPlayerCombo: Int = 0
    private var arenaInteractionsCount: Int = 0
    private var battleFinished: Boolean = false
    private var playerWonMatch: Boolean = false

    init {
        startBattle(
            playerProfile = FighterProfilePlaceholder.defaultPlayer(),
            opponentProfile = FighterProfilePlaceholder.defaultOpponent(),
            stage = 1,
            aiDiff = AiDifficulty.WARRIOR
        )
    }

    fun startBattle(
        playerProfile: FighterProfile,
        opponentProfile: FighterProfile,
        stage: Int,
        aiDiff: AiDifficulty
    ) {
        synchronized(lock) {
            stageNumber = stage
            difficulty = aiDiff
            player = FighterActor(playerProfile, isPlayer = true, x = -2.8f, facingSign = 1f)
            opponent = FighterActor(opponentProfile, isPlayer = false, x = 2.8f, facingSign = -1f)

            pillars.clear()
            // Four interactive cyber-stone pillars positioned at tactical flanks and mid-back lanes
            pillars.add(ArenaPillar(id = 1, x = -5.8f, z = -0.95f, neonHex = 0xFF00E5FF.toInt()))
            pillars.add(ArenaPillar(id = 2, x = -2.1f, z = -1.35f, neonHex = 0xFF00B0FF.toInt()))
            pillars.add(ArenaPillar(id = 3, x = 2.1f, z = -1.35f, neonHex = 0xFFFF1744.toInt()))
            pillars.add(ArenaPillar(id = 4, x = 5.8f, z = -0.95f, neonHex = 0xFFFF3D00.toInt()))

            zoneEffects.clear()
            projectiles.clear()
            particles.clear()

            introTimer = 2.0f
            roundTimer = 99f
            elapsedMatchTime = 0f
            bossPhase = 1
            bossPendingMajorMove = MoveType.NONE
            bossTelegraphTimer = 0f
            bossWarningText = ""
            bossWarningTimer = 0f

            bannerText = if (opponentProfile.isBoss) {
                "FINAL STAGE — SHADOW DRAGON OVERLORD"
            } else {
                "BATTLE $stage — ${playerProfile.displayName} VS ${opponentProfile.displayName}"
            }
            bannerTimer = 2.2f

            totalDamageDealt = 0f
            maxPlayerCombo = 0
            arenaInteractionsCount = 0
            battleFinished = false
            playerWonMatch = false
            isPaused = false
            inputMoveX = 0f
            inputBlockHeld = false
            cameraZoomUltimate = 0f
            cameraShakeIntensity = 0f
            arenaFlashIntensity = 0f

            audio?.play(SoundCue.FIGHT_ANNOUNCE)
        }
    }

    fun requestPlayerJump() {
        synchronized(lock) {
            if (isPaused || battleFinished || introTimer > 0f) return
            tryJump(player)
        }
    }

    fun requestPlayerDodge() {
        synchronized(lock) {
            if (isPaused || battleFinished || introTimer > 0f) return
            val dir = when {
                abs(inputMoveX) > 0.2f -> if (inputMoveX > 0f) 1f else -1f
                else -> -player.facingSign
            }
            tryDodge(player, dir)
        }
    }

    fun requestPlayerAttack(moveType: MoveType) {
        synchronized(lock) {
            if (isPaused || battleFinished || introTimer > 0f) return
            tryStartAttack(player, opponent, moveType)
        }
    }

    fun step(rawDt: Float): CombatHudSnapshot {
        synchronized(lock) {
            val dt = rawDt.coerceIn(0.001f, 0.05f)
            if (isPaused) {
                return buildHudSnapshot()
            }

            // Decay camera & arena flashes
            cameraShakeIntensity = max(0f, cameraShakeIntensity - dt * 2.6f)
            arenaFlashIntensity = max(0f, arenaFlashIntensity - dt * 2.8f)
            val targetZoom = if (player.state == ActionState.ULTIMATE_CAST || opponent.state == ActionState.ULTIMATE_CAST) 1f else 0f
            cameraZoomUltimate += (targetZoom - cameraZoomUltimate) * min(1f, dt * 6f)

            updateParticles(dt)
            updatePillars(dt)

            if (bannerTimer > 0f) {
                bannerTimer = max(0f, bannerTimer - dt)
                if (bannerTimer <= 0f && !battleFinished) bannerText = ""
            }
            if (bossWarningTimer > 0f) {
                bossWarningTimer = max(0f, bossWarningTimer - dt)
                if (bossWarningTimer <= 0f) bossWarningText = ""
            }

            if (introTimer > 0f) {
                introTimer -= dt
                player.animTime += dt
                opponent.animTime += dt
                if (introTimer <= 0f) {
                    player.state = ActionState.IDLE
                    opponent.state = ActionState.IDLE
                    bannerText = "FIGHT!"
                    bannerTimer = 1.0f
                }
                return buildHudSnapshot()
            }

            if (!battleFinished) {
                elapsedMatchTime += dt
                roundTimer = max(0f, roundTimer - dt)

                updateZoneEffects(dt)
                updatePlayerInput(dt)
                updateAiOpponent(dt)

                updateActorPhysicsAndCombat(player, opponent, dt)
                updateActorPhysicsAndCombat(opponent, player, dt)
                resolveFighterBodySeparation()
                updateProjectiles(dt)

                // Check Boss Phase 2 transition (< 50% HP)
                if (opponent.profile.isBoss && bossPhase == 1 && opponent.hp > 0f && opponent.hp <= opponent.profile.maxHealth * 0.5f) {
                    triggerBossPhaseTwo()
                }

                // Check victory / defeat / timeout
                if (opponent.hp <= 0f || player.hp <= 0f || roundTimer <= 0f) {
                    concludeMatch()
                }
            } else {
                player.animTime += dt
                opponent.animTime += dt
                updateGravityOnly(player, dt)
                updateGravityOnly(opponent, dt)
            }

            return buildHudSnapshot()
        }
    }

    private fun updatePlayerInput(dt: Float) {
        if (!canControlMovement(player)) return

        if (inputBlockHeld && player.isGrounded) {
            player.state = ActionState.BLOCK
            player.vx = 0f
            return
        } else if (player.state == ActionState.BLOCK) {
            player.state = ActionState.IDLE
        }

        val move = inputMoveX
        if (abs(move) > 0.15f) {
            val speedScale = if (player.slowedByFloorCrackTimer > 0f) 0.58f else 1.0f
            val runBoost = if (abs(move) > 0.82f) 1.22f else 1.0f
            player.vx = move * player.profile.moveSpeed * speedScale * runBoost
            if (player.isGrounded) {
                val movingForward = (move * player.facingSign) > 0f
                player.state = when {
                    abs(move) > 0.82f && movingForward -> ActionState.RUN
                    movingForward -> ActionState.WALK_FORWARD
                    else -> ActionState.WALK_BACKWARD
                }
                // Running across arena subtly kicks up dust
                if (player.state == ActionState.RUN && rng.nextFloat() < dt * 9f) {
                    spawnDustPuff(player.x, 0.06f, 0f, 3)
                }
            }
        } else {
            player.vx = 0f
            if (player.isGrounded && player.state != ActionState.BLOCK) {
                player.state = ActionState.IDLE
            }
        }
    }

    private fun updateAiOpponent(dt: Float) {
        if (opponent.hp <= 0f) return

        // Handle Boss telegraphed major attack countdown
        if (opponent.profile.isBoss && bossPendingMajorMove != MoveType.NONE) {
            bossTelegraphTimer -= dt
            opponent.specialGlowTimer = 0.25f
            if (bossTelegraphTimer <= 0f) {
                val move = bossPendingMajorMove
                bossPendingMajorMove = MoveType.NONE
                if (canInitiateAttack(opponent)) {
                    tryStartAttack(opponent, player, move)
                }
            }
            return
        }

        if (!canControlMovement(opponent)) return

        if (aiHoldBlockTimer > 0f) {
            aiHoldBlockTimer -= dt
            opponent.state = ActionState.BLOCK
            opponent.vx = 0f
            return
        } else if (opponent.state == ActionState.BLOCK) {
            opponent.state = ActionState.IDLE
        }

        aiDecisionTimer -= dt
        val dist = abs(player.x - opponent.x)
        val dirToPlayer = if (player.x >= opponent.x) 1f else -1f

        // Check if player is cloaked inside a smoke cloud -> AI accuracy & reaction drops!
        val playerHiddenInSmoke = player.stealthSmokeCloakTimer > 0f

        if (aiDecisionTimer <= 0f) {
            val phaseMultiplier = if (opponent.profile.isBoss && bossPhase == 2) 1.35f else 1.0f
            val baseInterval = (0.34f / (difficulty.reactionScale * phaseMultiplier)).coerceIn(0.16f, 0.55f)
            aiDecisionTimer = if (playerHiddenInSmoke) baseInterval * 1.65f else baseInterval

            // Defensive reaction if player is mid-attack or firing projectile nearby
            val incomingThreat = (player.state == ActionState.ATTACK_STARTUP || player.state == ActionState.ATTACK_ACTIVE) && dist < 2.7f
            val incomingProj = projectiles.any { it.ownerIsPlayer && abs(it.x - opponent.x) < 2.6f }

            if (!playerHiddenInSmoke && (incomingThreat || incomingProj) && rng.nextFloat() < 0.42f * difficulty.reactionScale) {
                if (rng.nextBoolean()) {
                    aiHoldBlockTimer = 0.36f
                    return
                } else {
                    tryDodge(opponent, -dirToPlayer)
                    return
                }
            }

            // Ultimate check for normal AI or Boss
            if (opponent.ultimateEnergy >= 100f && dist < 4.5f && !playerHiddenInSmoke && rng.nextFloat() < 0.55f) {
                if (opponent.profile.isBoss) {
                    scheduleBossMajorMove(MoveType.BOSS_DARK_SHOCKWAVE)
                } else {
                    tryStartAttack(opponent, player, MoveType.ULTIMATE)
                }
                return
            }

            // Boss-specific attack suite
            if (opponent.profile.isBoss) {
                val roll = rng.nextFloat()
                if (dist <= opponent.profile.attackReach + 0.35f) {
                    when {
                        roll < 0.26f -> tryStartAttack(opponent, player, MoveType.PUNCH) // Heavy claw combo
                        roll < 0.48f -> tryStartAttack(opponent, player, MoveType.BOSS_TAIL_SWEEP)
                        roll < 0.70f -> scheduleBossMajorMove(MoveType.HEAVY) // Cataclysm Ground Slam
                        roll < 0.88f && bossPhase == 2 -> scheduleBossMajorMove(MoveType.BOSS_DARK_SHOCKWAVE)
                        else -> scheduleBossMajorMove(MoveType.BOSS_FLAME_BREATH)
                    }
                    return
                } else if (dist < 5.6f && roll < 0.48f * phaseMultiplier) {
                    if (bossPhase == 2 && roll < 0.22f) {
                        scheduleBossMajorMove(MoveType.BOSS_FLAME_BREATH)
                    } else {
                        tryStartAttack(opponent, player, MoveType.PROJECTILE) // Dark fireball
                    }
                    return
                }
            } else {
                // Normal animal warrior AI
                val roll = rng.nextFloat() * difficulty.aggressionScale
                if (dist <= opponent.profile.attackReach + 0.25f && !playerHiddenInSmoke) {
                    when {
                        roll > 0.72f -> tryStartAttack(opponent, player, MoveType.SPECIAL_MELEE)
                        roll > 0.48f -> tryStartAttack(opponent, player, MoveType.HEAVY)
                        roll > 0.24f -> tryStartAttack(opponent, player, MoveType.KICK)
                        else -> tryStartAttack(opponent, player, MoveType.PUNCH)
                    }
                    return
                } else if (dist > 3.2f && roll > 0.58f && opponent.projectileCooldown <= 0f) {
                    tryStartAttack(opponent, player, MoveType.PROJECTILE)
                    return
                }
            }

            // Choose movement direction
            aiDesiredMoveDir = when {
                playerHiddenInSmoke -> if (rng.nextBoolean()) -dirToPlayer * 0.5f else 0f
                dist > opponent.profile.attackReach * 0.88f -> dirToPlayer
                dist < 1.15f -> -dirToPlayer * 0.6f
                else -> 0f
            }

            if (dist > 3.6f && rng.nextFloat() < 0.14f) {
                tryJump(opponent)
            }
        }

        if (abs(aiDesiredMoveDir) > 0.1f) {
            val speedScale = if (opponent.slowedByFloorCrackTimer > 0f) 0.58f else 1.0f
            val bossSpeedBoost = if (opponent.profile.isBoss && bossPhase == 2) 1.22f else 1.0f
            opponent.vx = aiDesiredMoveDir * opponent.profile.moveSpeed * speedScale * bossSpeedBoost
            if (opponent.isGrounded) {
                opponent.state = if (aiDesiredMoveDir * opponent.facingSign > 0f) {
                    ActionState.WALK_FORWARD
                } else {
                    ActionState.WALK_BACKWARD
                }
            }
        } else {
            opponent.vx = 0f
            if (opponent.isGrounded) opponent.state = ActionState.IDLE
        }
    }

    private fun scheduleBossMajorMove(move: MoveType) {
        bossPendingMajorMove = move
        bossTelegraphTimer = if (bossPhase == 2) 0.55f else 0.72f
        bossWarningText = when (move) {
            MoveType.BOSS_DARK_SHOCKWAVE -> "WARNING: ABYSSAL SHOCKWAVE — DODGE OR BLOCK!"
            MoveType.BOSS_FLAME_BREATH -> "WARNING: DRAGON FLAME BREATH INCOMING!"
            else -> "WARNING: OVERLORD TECTONIC SLAM!"
        }
        bossWarningTimer = bossTelegraphTimer + 0.35f
        spawnBurstParticles(opponent.x, 1.5f, 0f, 0xFFFF0033.toInt(), 14, 4.2f)
        audio?.play(SoundCue.BOSS_ROAR)
    }

    private fun triggerBossPhaseTwo() {
        bossPhase = 2
        opponent.specialGlowTimer = 4.0f
        bannerText = "PHASE 2 — OVERLORD FURY AWAKENED!"
        bannerTimer = 2.2f
        bossWarningText = "SHADOW DRAGON OVERLORD SHATTERS THE ARENA!"
        bossWarningTimer = 2.4f
        triggerCameraShake(0.85f)
        triggerArenaFlash(0xFFFF0033.toInt(), 0.75f)
        audio?.play(SoundCue.BOSS_ROAR)

        // Phase 2 transition shockwave damages nearby intact pillar and spawns scorched fissure
        damageNearbyPillars(opponent.x, radius = 3.8f, integrityDamage = 65f, attacker = opponent, defender = player)
        spawnZoneEffect(
            type = ArenaHazardType.SCORCHED_FLAME_ZONE,
            x = opponent.x,
            z = 0f,
            radius = 2.0f,
            duration = 6.5f,
            ownerIsPlayer = false
        )
    }

    private fun canControlMovement(actor: FighterActor): Boolean {
        return actor.state == ActionState.IDLE ||
            actor.state == ActionState.WALK_FORWARD ||
            actor.state == ActionState.WALK_BACKWARD ||
            actor.state == ActionState.RUN ||
            actor.state == ActionState.JUMP ||
            actor.state == ActionState.BLOCK
    }

    private fun canInitiateAttack(actor: FighterActor): Boolean {
        return canControlMovement(actor) && actor.state != ActionState.HIT_STUN && actor.state != ActionState.KNOCKDOWN
    }

    private fun tryJump(actor: FighterActor) {
        if (!actor.isGrounded || !canControlMovement(actor)) return
        actor.vy = actor.profile.jumpVelocity
        actor.y = 0.03f
        actor.state = ActionState.JUMP
        actor.stateTimer = 0f
        spawnDustPuff(actor.x, 0.05f, 0f, 6)
        audio?.play(SoundCue.WHOOSH_DODGE)
    }

    private fun tryDodge(actor: FighterActor, dir: Float) {
        if (actor.dodgeCooldown > 0f || !canInitiateAttack(actor)) return
        actor.state = ActionState.DODGE
        actor.stateTimer = 0f
        actor.dodgeCooldown = 0.75f
        actor.vx = dir * (actor.profile.moveSpeed * 1.85f)
        // Dodging sharply kicks up tactical dust cloud for Shadow Tiger or any agile roll
        spawnDustPuff(actor.x, 0.12f, 0f, 10)
        if (actor.profile.id == FighterId.SHADOW_TIGER) {
            spawnZoneEffect(
                type = ArenaHazardType.DUST_SMOKE_CLOUD,
                x = actor.x,
                z = 0f,
                radius = 1.65f,
                duration = 3.8f,
                ownerIsPlayer = actor.isPlayer
            )
            actor.stealthSmokeCloakTimer = max(actor.stealthSmokeCloakTimer, 2.2f)
            if (actor.isPlayer) arenaInteractionsCount++
        }
        audio?.play(SoundCue.WHOOSH_DODGE)
    }

    private fun tryStartAttack(attacker: FighterActor, defender: FighterActor, moveType: MoveType) {
        if (!canInitiateAttack(attacker)) return

        // Face opponent on attack start
        attacker.facingSign = if (defender.x >= attacker.x) 1f else -1f

        when (moveType) {
            MoveType.PUNCH -> {
                if (attacker.lightCooldown > 0f) return
                attacker.lightCooldown = if (attacker.profile.id == FighterId.SHADOW_TIGER) 0.24f else 0.34f
                startAttackState(attacker, MoveType.PUNCH, duration = 0.30f, lungeSpeed = 1.8f)
            }
            MoveType.KICK -> {
                if (attacker.lightCooldown > 0f) return
                attacker.lightCooldown = 0.42f
                startAttackState(attacker, MoveType.KICK, duration = 0.38f, lungeSpeed = 2.6f)
            }
            MoveType.HEAVY -> {
                if (attacker.heavyCooldown > 0f) return
                attacker.heavyCooldown = 0.72f
                attacker.specialGlowTimer = 0.55f
                startAttackState(attacker, MoveType.HEAVY, duration = 0.54f, lungeSpeed = 2.4f)
            }
            MoveType.SPECIAL_MELEE -> {
                // Hybrid behavior: if opponent is far and projectile is off cooldown, launch signature projectile;
                // otherwise execute signature animal melee special (or explicit MoveType.PROJECTILE)
                val dist = abs(defender.x - attacker.x)
                if (dist > 3.3f && attacker.projectileCooldown <= 0f) {
                    tryStartAttack(attacker, defender, MoveType.PROJECTILE)
                    return
                }
                if (attacker.specialCooldown > 0f) return
                attacker.specialCooldown = 1.15f
                attacker.specialGlowTimer = 0.85f
                val lunge = when (attacker.profile.id) {
                    FighterId.IRON_BUFFALO -> 6.8f  // Charging stampede
                    FighterId.TITAN_RHINO -> 6.4f   // Armored body charge
                    FighterId.SHADOW_TIGER -> 8.2f  // Tiger pounce
                    FighterId.INFERNO_DRAGON -> 4.2f // Flame breath + wing rush
                    FighterId.SHADOW_DRAGON_OVERLORD -> 5.0f
                }
                startAttackState(attacker, MoveType.SPECIAL_MELEE, duration = 0.62f, lungeSpeed = lunge)
            }
            MoveType.PROJECTILE -> {
                if (attacker.projectileCooldown > 0f) return
                attacker.projectileCooldown = 1.25f
                attacker.specialGlowTimer = 0.65f
                startAttackState(attacker, MoveType.PROJECTILE, duration = 0.44f, lungeSpeed = 0f)
            }
            MoveType.ULTIMATE -> {
                if (attacker.ultimateEnergy < 100f) return
                attacker.ultimateEnergy = 0f
                attacker.state = ActionState.ULTIMATE_CAST
                attacker.stateTimer = 0f
                attacker.currentMove = MoveType.ULTIMATE
                attacker.moveDuration = 1.35f
                attacker.moveHitApplied = false
                attacker.ultimateSubHitsRemaining = 3
                attacker.ultimateNextHitTime = 0.28f
                attacker.specialGlowTimer = 1.8f
                attacker.vx = attacker.facingSign * 5.8f
                triggerArenaFlash(attacker.profile.glowHex, 0.85f)
                triggerCameraShake(0.65f)
                bannerText = "${attacker.profile.ultimateName}!"
                bannerTimer = 1.5f
                audio?.playUltimateFor(attacker.profile.id)
                spawnBurstParticles(attacker.x, 1.2f, 0f, attacker.profile.glowHex, 24, 6.0f)
            }
            MoveType.BOSS_TAIL_SWEEP -> {
                startAttackState(attacker, MoveType.BOSS_TAIL_SWEEP, duration = 0.50f, lungeSpeed = 2.8f)
            }
            MoveType.BOSS_FLAME_BREATH -> {
                attacker.specialGlowTimer = 1.0f
                startAttackState(attacker, MoveType.BOSS_FLAME_BREATH, duration = 0.68f, lungeSpeed = 1.5f)
            }
            MoveType.BOSS_DARK_SHOCKWAVE -> {
                attacker.ultimateEnergy = 0f
                attacker.specialGlowTimer = 1.4f
                startAttackState(attacker, MoveType.BOSS_DARK_SHOCKWAVE, duration = 0.82f, lungeSpeed = 0f)
            }
            MoveType.NONE -> Unit
        }
    }

    private fun startAttackState(
        attacker: FighterActor,
        move: MoveType,
        duration: Float,
        lungeSpeed: Float
    ) {
        attacker.state = ActionState.ATTACK_STARTUP
        attacker.stateTimer = 0f
        attacker.currentMove = move
        attacker.moveDuration = duration
        attacker.moveHitApplied = false
        attacker.vx = attacker.facingSign * lungeSpeed
        audio?.play(SoundCue.WHOOSH_DODGE)
    }

    private fun updateActorPhysicsAndCombat(actor: FighterActor, target: FighterActor, dt: Float) {
        actor.animTime += dt
        actor.stateTimer += dt

        // Cooldowns & status timers
        actor.lightCooldown = max(0f, actor.lightCooldown - dt)
        actor.heavyCooldown = max(0f, actor.heavyCooldown - dt)
        actor.specialCooldown = max(0f, actor.specialCooldown - dt)
        actor.projectileCooldown = max(0f, actor.projectileCooldown - dt)
        actor.dodgeCooldown = max(0f, actor.dodgeCooldown - dt)
        actor.hitFlashTimer = max(0f, actor.hitFlashTimer - dt)
        actor.specialGlowTimer = max(0f, actor.specialGlowTimer - dt)
        actor.stealthSmokeCloakTimer = max(0f, actor.stealthSmokeCloakTimer - dt)
        actor.slowedByFloorCrackTimer = max(0f, actor.slowedByFloorCrackTimer - dt)

        if (actor.comboDecayTimer > 0f) {
            actor.comboDecayTimer -= dt
            if (actor.comboDecayTimer <= 0f) actor.comboCount = 0
        }

        // Update auto-facing when idle/walking
        if (actor.state == ActionState.IDLE || actor.state == ActionState.WALK_FORWARD || actor.state == ActionState.WALK_BACKWARD || actor.state == ActionState.BLOCK) {
            actor.facingSign = if (target.x >= actor.x) 1f else -1f
        }

        // Horizontal integration + arena bounds
        actor.x = (actor.x + actor.vx * dt).coerceIn(-arenaHalfWidth + 0.65f, arenaHalfWidth - 0.65f)

        // Vertical gravity
        if (!actor.isGrounded || actor.vy > 0f) {
            actor.y += actor.vy * dt
            actor.vy += gravity * dt
            if (actor.y <= 0f) {
                actor.y = 0f
                actor.vy = 0f
                spawnDustPuff(actor.x, 0.05f, 0f, 6)
                if (actor.state == ActionState.JUMP) {
                    actor.state = ActionState.IDLE
                }
            }
        }

        // State machine transitions
        when (actor.state) {
            ActionState.DODGE -> {
                if (actor.stateTimer >= 0.36f) {
                    actor.vx = 0f
                    actor.state = ActionState.IDLE
                }
            }
            ActionState.HIT_STUN -> {
                actor.vx *= (1f - min(1f, dt * 8f))
                if (actor.stateTimer >= 0.34f) {
                    actor.vx = 0f
                    actor.state = ActionState.IDLE
                }
            }
            ActionState.KNOCKDOWN -> {
                actor.vx *= (1f - min(1f, dt * 6f))
                if (actor.stateTimer >= 0.72f) {
                    actor.vx = 0f
                    actor.state = ActionState.IDLE
                }
            }
            ActionState.ATTACK_STARTUP,
            ActionState.ATTACK_ACTIVE,
            ActionState.ATTACK_RECOVERY -> {
                val startupEnd = actor.moveDuration * 0.28f
                val activeEnd = actor.moveDuration * 0.66f

                actor.vx *= (1f - min(1f, dt * 4.2f))

                if (actor.stateTimer < startupEnd) {
                    actor.state = ActionState.ATTACK_STARTUP
                } else if (actor.stateTimer < activeEnd) {
                    actor.state = ActionState.ATTACK_ACTIVE
                    if (!actor.moveHitApplied) {
                        actor.moveHitApplied = true
                        executeMoveActiveImpact(actor, target)
                    }
                } else if (actor.stateTimer < actor.moveDuration) {
                    actor.state = ActionState.ATTACK_RECOVERY
                } else {
                    actor.vx = 0f
                    actor.currentMove = MoveType.NONE
                    actor.state = ActionState.IDLE
                }
            }
            ActionState.ULTIMATE_CAST -> {
                // Multi-hit cinematic rush + massive finisher + 0.35s recovery window
                if (actor.ultimateSubHitsRemaining > 0 && actor.stateTimer >= actor.ultimateNextHitTime) {
                    val isFinalBlow = actor.ultimateSubHitsRemaining == 1
                    actor.ultimateSubHitsRemaining--
                    actor.ultimateNextHitTime += 0.26f
                    executeUltimateSubHit(actor, target, isFinalBlow)
                }
                if (actor.stateTimer > 0.95f) {
                    actor.vx *= (1f - min(1f, dt * 9f))
                } else {
                    // Track toward target during Ultimate charge
                    val gap = target.x - actor.x
                    if (abs(gap) > 1.25f) {
                        actor.vx = (if (gap >= 0f) 1f else -1f) * 6.8f
                    } else {
                        actor.vx = 0f
                    }
                }
                if (actor.stateTimer >= actor.moveDuration) {
                    actor.vx = 0f
                    actor.currentMove = MoveType.NONE
                    // Recovery period after activation so player retains fair tactical rhythm
                    actor.state = ActionState.ATTACK_RECOVERY
                    actor.stateTimer = 0f
                    actor.moveDuration = 0.28f
                    actor.moveHitApplied = true
                }
            }
            else -> Unit
        }
    }

    private fun executeMoveActiveImpact(attacker: FighterActor, defender: FighterActor) {
        val move = attacker.currentMove
        if (move == MoveType.PROJECTILE) {
            spawnProjectile(attacker)
            return
        }

        // Trigger signature environmental interactions!
        handleEnvironmentalMoveEffects(attacker, defender, move)

        val reach = when (move) {
            MoveType.PUNCH -> attacker.profile.attackReach * 0.92f
            MoveType.KICK -> attacker.profile.attackReach * 1.06f
            MoveType.HEAVY -> attacker.profile.attackReach * 1.18f
            MoveType.SPECIAL_MELEE -> attacker.profile.attackReach * 1.32f
            MoveType.BOSS_TAIL_SWEEP -> 2.85f
            MoveType.BOSS_FLAME_BREATH -> 3.65f
            MoveType.BOSS_DARK_SHOCKWAVE -> 5.20f
            else -> attacker.profile.attackReach
        }

        val dx = defender.x - attacker.x
        val inFrontOrShockwave = (move == MoveType.BOSS_DARK_SHOCKWAVE) || (dx * attacker.facingSign >= -0.45f)
        val verticalOverlap = abs(defender.y - attacker.y) < 1.55f

        if (inFrontOrShockwave && abs(dx) <= reach && verticalOverlap) {
            val baseDamage = when (move) {
                MoveType.PUNCH -> 6.5f
                MoveType.KICK -> 8.5f
                MoveType.HEAVY -> 13.5f
                MoveType.SPECIAL_MELEE -> 15.5f
                MoveType.BOSS_TAIL_SWEEP -> 14.0f
                MoveType.BOSS_FLAME_BREATH -> 18.0f
                MoveType.BOSS_DARK_SHOCKWAVE -> 24.0f
                else -> 8.0f
            }
            val knockback = when (move) {
                MoveType.PUNCH -> 1.8f
                MoveType.KICK -> 2.8f
                MoveType.HEAVY -> 4.6f
                MoveType.SPECIAL_MELEE -> 5.2f
                MoveType.BOSS_DARK_SHOCKWAVE -> 6.8f
                else -> 3.8f
            }
            val causesKnockdown = move == MoveType.HEAVY || move == MoveType.BOSS_DARK_SHOCKWAVE || move == MoveType.BOSS_TAIL_SWEEP
            applyHitToTarget(
                attacker = attacker,
                defender = defender,
                rawBaseDamage = baseDamage,
                knockbackSpeed = knockback,
                heavyImpact = move != MoveType.PUNCH,
                causesKnockdown = causesKnockdown
            )
        }
    }

    private fun executeUltimateSubHit(attacker: FighterActor, defender: FighterActor, isFinalBlow: Boolean) {
        val dist = abs(defender.x - attacker.x)
        val reach = attacker.profile.attackReach + 1.65f

        spawnBurstParticles(
            x = attacker.x + attacker.facingSign * 1.0f,
            y = 1.1f,
            z = 0f,
            colorHex = attacker.profile.glowHex,
            count = if (isFinalBlow) 28 else 14,
            speed = if (isFinalBlow) 7.5f else 4.5f
        )

        if (isFinalBlow) {
            triggerCameraShake(0.95f)
            triggerArenaFlash(attacker.profile.glowHex, 0.9f)
            // Ultimate final impact interacts dramatically with the Beast Battle Arena!
            damageNearbyPillars(attacker.x + attacker.facingSign * 1.1f, radius = 3.2f, integrityDamage = 100f, attacker = attacker, defender = defender)
            when (attacker.profile.id) {
                FighterId.IRON_BUFFALO -> {
                    spawnZoneEffect(ArenaHazardType.SHATTERED_FLOOR, defender.x, 0f, 2.2f, 8.0f, attacker.isPlayer)
                    audio?.play(SoundCue.FLOOR_CRACK)
                }
                FighterId.TITAN_RHINO -> {
                    spawnZoneEffect(ArenaHazardType.SHATTERED_FLOOR, defender.x, 0f, 2.0f, 7.0f, attacker.isPlayer)
                    spawnDustPuff(defender.x, 0.2f, 0f, 22)
                    audio?.play(SoundCue.FLOOR_CRACK)
                }
                FighterId.SHADOW_TIGER -> {
                    spawnZoneEffect(ArenaHazardType.DUST_SMOKE_CLOUD, attacker.x, 0f, 2.3f, 5.5f, attacker.isPlayer)
                    attacker.stealthSmokeCloakTimer = 3.2f
                }
                FighterId.INFERNO_DRAGON -> {
                    spawnZoneEffect(ArenaHazardType.SCORCHED_FLAME_ZONE, defender.x, 0f, 2.2f, 6.5f, attacker.isPlayer)
                }
                FighterId.SHADOW_DRAGON_OVERLORD -> {
                    spawnZoneEffect(ArenaHazardType.SCORCHED_FLAME_ZONE, defender.x, 0f, 2.5f, 6.5f, false)
                }
            }
            if (attacker.isPlayer) arenaInteractionsCount++
        }

        if (dist <= reach) {
            val subDamage = if (isFinalBlow) 14.5f else 6.5f // ~27.5 base * attackPower (~30-35 total HP if unblocked)
            applyHitToTarget(
                attacker = attacker,
                defender = defender,
                rawBaseDamage = subDamage,
                knockbackSpeed = if (isFinalBlow) 7.2f else 1.6f,
                heavyImpact = true,
                causesKnockdown = isFinalBlow
            )
        }
    }

    private fun handleEnvironmentalMoveEffects(
        attacker: FighterActor,
        defender: FighterActor,
        move: MoveType
    ) {
        val strikeX = attacker.x + attacker.facingSign * 1.15f

        // 1. Heavy, Special, and Boss slams damage nearby destructible arena pillars
        if (move == MoveType.HEAVY || move == MoveType.SPECIAL_MELEE || move == MoveType.BOSS_DARK_SHOCKWAVE || move == MoveType.BOSS_TAIL_SWEEP) {
            val pillarDmg = if (move == MoveType.HEAVY || move == MoveType.BOSS_DARK_SHOCKWAVE) 55f else 45f
            damageNearbyPillars(strikeX, radius = 2.15f, integrityDamage = pillarDmg, attacker = attacker, defender = defender)
        }

        // 2. Character-signature tactical arena interactions
        when (attacker.profile.id) {
            FighterId.IRON_BUFFALO -> {
                if (move == MoveType.HEAVY) {
                    // Ground-Shaking Stomp shatters arena floor plates & kicks up dust!
                    spawnZoneEffect(
                        type = ArenaHazardType.SHATTERED_FLOOR,
                        x = strikeX,
                        z = 0f,
                        radius = 1.85f,
                        duration = 7.5f,
                        ownerIsPlayer = attacker.isPlayer
                    )
                    spawnDustPuff(strikeX, 0.1f, 0f, 16)
                    triggerCameraShake(0.50f)
                    audio?.play(SoundCue.FLOOR_CRACK)
                    if (attacker.isPlayer) arenaInteractionsCount++
                }
            }
            FighterId.TITAN_RHINO -> {
                if (move == MoveType.HEAVY || move == MoveType.SPECIAL_MELEE) {
                    // Tectonic Ground Slam kicks up rock splinters & cracks floor
                    spawnZoneEffect(
                        type = ArenaHazardType.SHATTERED_FLOOR,
                        x = strikeX,
                        z = 0f,
                        radius = 1.60f,
                        duration = 6.0f,
                        ownerIsPlayer = attacker.isPlayer
                    )
                    spawnBurstParticles(strikeX, 0.2f, 0f, 0xFF90A4AE.toInt(), 14, 5.2f)
                    triggerCameraShake(0.45f)
                    audio?.play(SoundCue.FLOOR_CRACK)
                    if (attacker.isPlayer) arenaInteractionsCount++
                }
            }
            FighterId.SHADOW_TIGER -> {
                if (move == MoveType.KICK || move == MoveType.SPECIAL_MELEE) {
                    // Cyclone Kick / Pounce kicks up arena dust & smoke from broken structures!
                    val nearBrokenPillar = pillars.any { it.isBroken && abs(it.x - attacker.x) < 2.8f }
                    val radius = if (nearBrokenPillar) 2.15f else 1.55f
                    spawnZoneEffect(
                        type = ArenaHazardType.DUST_SMOKE_CLOUD,
                        x = attacker.x + attacker.facingSign * 0.6f,
                        z = 0f,
                        radius = radius,
                        duration = if (nearBrokenPillar) 5.0f else 3.4f,
                        ownerIsPlayer = attacker.isPlayer
                    )
                    attacker.stealthSmokeCloakTimer = max(attacker.stealthSmokeCloakTimer, 2.5f)
                    audio?.play(SoundCue.SMOKE_BURST)
                    if (attacker.isPlayer) arenaInteractionsCount++
                }
            }
            FighterId.INFERNO_DRAGON -> {
                if (move == MoveType.SPECIAL_MELEE || move == MoveType.HEAVY) {
                    // Fire Breath ignites shattered floor fissures or broken pillars into Scorched Flame Zones!
                    val existingCrack = zoneEffects.firstOrNull {
                        it.type == ArenaHazardType.SHATTERED_FLOOR && abs(it.x - strikeX) < 2.4f
                    }
                    val nearBrokenPillar = pillars.any { it.isBroken && abs(it.x - strikeX) < 2.5f }
                    if (existingCrack != null || nearBrokenPillar || move == MoveType.SPECIAL_MELEE) {
                        spawnZoneEffect(
                            type = ArenaHazardType.SCORCHED_FLAME_ZONE,
                            x = existingCrack?.x ?: strikeX,
                            z = 0f,
                            radius = 1.65f,
                            duration = 4.8f,
                            ownerIsPlayer = attacker.isPlayer
                        )
                        spawnBurstParticles(strikeX, 0.6f, 0f, 0xFFFF4500.toInt(), 16, 4.8f)
                        if (attacker.isPlayer) arenaInteractionsCount++
                    }
                }
            }
            FighterId.SHADOW_DRAGON_OVERLORD -> {
                if (move == MoveType.HEAVY || move == MoveType.BOSS_DARK_SHOCKWAVE) {
                    spawnZoneEffect(
                        type = ArenaHazardType.SHATTERED_FLOOR,
                        x = strikeX,
                        z = 0f,
                        radius = 2.0f,
                        duration = 6.5f,
                        ownerIsPlayer = false
                    )
                    triggerCameraShake(0.62f)
                    audio?.play(SoundCue.FLOOR_CRACK)
                } else if (move == MoveType.BOSS_FLAME_BREATH) {
                    spawnZoneEffect(
                        type = ArenaHazardType.SCORCHED_FLAME_ZONE,
                        x = strikeX,
                        z = 0f,
                        radius = 1.85f,
                        duration = 5.0f,
                        ownerIsPlayer = false
                    )
                }
            }
        }
    }

    private fun damageNearbyPillars(
        impactX: Float,
        radius: Float,
        integrityDamage: Float,
        attacker: FighterActor,
        defender: FighterActor
    ) {
        for (pillar in pillars) {
            if (!pillar.isBroken && abs(pillar.x - impactX) <= radius) {
                pillar.integrity = max(0f, pillar.integrity - integrityDamage)
                pillar.hitFlash = 0.35f
                spawnBurstParticles(pillar.x, 1.4f, pillar.z, pillar.neonHex, 12, 4.5f)

                if (pillar.isBroken) {
                    // Pillar shatters! Creates billowing smoke cloud & shrapnel blast
                    audio?.play(SoundCue.PILLAR_SHATTER)
                    triggerCameraShake(0.68f)
                    triggerArenaFlash(pillar.neonHex, 0.55f)
                    if (attacker.isPlayer) arenaInteractionsCount++

                    spawnBurstParticles(pillar.x, 1.8f, pillar.z, 0xFFCFD8DC.toInt(), 24, 6.8f)
                    spawnBurstParticles(pillar.x, 1.5f, pillar.z, pillar.neonHex, 18, 6.2f)

                    // Broken structure vents dense smoke for tactical cover
                    spawnZoneEffect(
                        type = ArenaHazardType.DUST_SMOKE_CLOUD,
                        x = pillar.x,
                        z = 0f,
                        radius = 2.1f,
                        duration = 6.5f,
                        ownerIsPlayer = attacker.isPlayer
                    )

                    // Shrapnel hits opponent if standing near the collapsing pillar!
                    if (abs(defender.x - pillar.x) <= 2.5f && !defender.isInvulnerableDodge) {
                        val shrapnelBonus = if (attacker.profile.id == FighterId.TITAN_RHINO) 12.0f else 8.0f
                        applyHitToTarget(
                            attacker = attacker,
                            defender = defender,
                            rawBaseDamage = shrapnelBonus,
                            knockbackSpeed = 3.5f,
                            heavyImpact = true,
                            causesKnockdown = false
                        )
                    }
                }
            }
        }
    }

    private fun spawnZoneEffect(
        type: ArenaHazardType,
        x: Float,
        z: Float,
        radius: Float,
        duration: Float,
        ownerIsPlayer: Boolean
    ) {
        // Keep cap of 6 active zones for crisp 60fps performance
        if (zoneEffects.size >= 6) {
            zoneEffects.removeAt(0)
        }
        val clampedX = x.coerceIn(-arenaHalfWidth + 0.8f, arenaHalfWidth - 0.8f)
        zoneEffects.add(
            ArenaZoneEffect(
                id = nextEntityId++,
                type = type,
                x = clampedX,
                z = z,
                radius = radius,
                remainingTime = duration,
                maxTime = duration,
                ownerIsPlayer = ownerIsPlayer
            )
        )
    }

    private fun updateZoneEffects(dt: Float) {
        val iter = zoneEffects.iterator()
        while (iter.hasNext()) {
            val zone = iter.next()
            zone.remainingTime -= dt
            zone.tickTimer += dt
            if (zone.remainingTime <= 0f) {
                iter.remove()
                continue
            }

            // Ambient low-cost particles from active arena zones
            if (zone.tickTimer >= 0.22f) {
                zone.tickTimer = 0f
                when (zone.type) {
                    ArenaHazardType.DUST_SMOKE_CLOUD -> {
                        spawnDustPuff(zone.x + (rng.nextFloat() - 0.5f) * zone.radius, 0.35f, 0f, 2)
                        // Grant stealth smoke cloak to fighters inside smoke
                        if (abs(player.x - zone.x) <= zone.radius) {
                            player.stealthSmokeCloakTimer = max(player.stealthSmokeCloakTimer, 0.55f)
                        }
                        if (abs(opponent.x - zone.x) <= zone.radius && opponent.profile.id == FighterId.SHADOW_TIGER) {
                            opponent.stealthSmokeCloakTimer = max(opponent.stealthSmokeCloakTimer, 0.55f)
                        }
                    }
                    ArenaHazardType.SHATTERED_FLOOR -> {
                        // Grounded enemy stepping on opponent-created shattered floor is slowed!
                        val target = if (zone.ownerIsPlayer) opponent else player
                        if (target.isGrounded && abs(target.x - zone.x) <= zone.radius) {
                            target.slowedByFloorCrackTimer = 0.45f
                        }
                    }
                    ArenaHazardType.SCORCHED_FLAME_ZONE -> {
                        spawnBurstParticles(
                            x = zone.x + (rng.nextFloat() - 0.5f) * zone.radius,
                            y = 0.15f,
                            z = (rng.nextFloat() - 0.5f) * 0.5f,
                            colorHex = 0xFFFF5722.toInt(),
                            count = 2,
                            speed = 2.2f
                        )
                        val target = if (zone.ownerIsPlayer) opponent else player
                        if (target.isGrounded && !target.isInvulnerableDodge && abs(target.x - zone.x) <= zone.radius) {
                            val burnDmg = 0.9f * target.profile.defenseFactor
                            target.hp = max(0f, target.hp - burnDmg)
                            if (zone.ownerIsPlayer) totalDamageDealt += burnDmg
                        }
                    }
                    ArenaHazardType.DESTRUCTIBLE_PILLAR -> Unit
                }
            }
        }
    }

    private fun spawnProjectile(attacker: FighterActor) {
        val dir = attacker.facingSign
        val baseDmg = when (attacker.profile.id) {
            FighterId.IRON_BUFFALO -> 12.5f     // Compressed orange shockwave
            FighterId.TITAN_RHINO -> 11.5f      // Compressed gray rock blast
            FighterId.SHADOW_TIGER -> 9.5f      // Fast blue tiger-claw energy slash
            FighterId.INFERNO_DRAGON -> 13.5f   // Red-orange fireball
            FighterId.SHADOW_DRAGON_OVERLORD -> if (bossPhase == 2) 17.5f else 14.0f
        }
        projectiles.add(
            ProjectileEntity(
                id = nextEntityId++,
                ownerIsPlayer = attacker.isPlayer,
                sourceFighter = attacker.profile.id,
                x = attacker.x + dir * 0.95f,
                y = attacker.y + 1.15f,
                z = 0f,
                vx = dir * attacker.profile.projectileSpeed,
                radius = if (attacker.profile.isBoss) 0.48f else 0.36f,
                damage = baseDmg,
                colorHex = attacker.profile.glowHex,
                secondaryHex = attacker.profile.secondaryHex
            )
        )
        audio?.play(SoundCue.PROJECTILE_LAUNCH)
    }

    private fun updateProjectiles(dt: Float) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val proj = iter.next()
            proj.x += proj.vx * dt
            proj.life -= dt

            // Trail particle
            if (rng.nextFloat() < 0.65f) {
                addParticle(
                    Particle3D(
                        x = proj.x,
                        y = proj.y + (rng.nextFloat() - 0.5f) * 0.18f,
                        z = (rng.nextFloat() - 0.5f) * 0.18f,
                        vx = -proj.vx * 0.18f,
                        vy = (rng.nextFloat() - 0.5f) * 1.2f,
                        vz = (rng.nextFloat() - 0.5f) * 1.2f,
                        size = 0.14f,
                        colorHex = proj.colorHex,
                        life = 0.22f,
                        maxLife = 0.22f,
                        gravity = 0f
                    )
                )
            }

            if (proj.life <= 0f || abs(proj.x) > arenaHalfWidth + 1.2f) {
                iter.remove()
                continue
            }

            val attacker = if (proj.ownerIsPlayer) player else opponent
            val defender = if (proj.ownerIsPlayer) opponent else player

            // Projectile-vs-projectile clash!
            val opposing = projectiles.firstOrNull {
                it !== proj && it.ownerIsPlayer != proj.ownerIsPlayer && abs(it.x - proj.x) < (it.radius + proj.radius)
            }
            if (opposing != null) {
                opposing.life = 0f
                spawnBurstParticles(proj.x, proj.y, 0f, 0xFFFFFFFF.toInt(), 14, 5.0f)
                audio?.play(SoundCue.BLOCK_CLANG)
                iter.remove()
                continue
            }

            // Projectile collision with target body
            val hitX = abs(proj.x - defender.x) <= (proj.radius + 0.58f)
            val hitY = proj.y >= defender.y - 0.1f && proj.y <= defender.y + 2.15f
            if (hitX && hitY) {
                if (defender.isInvulnerableDodge) {
                    // Dodged right through projectile!
                    continue
                }
                applyHitToTarget(
                    attacker = attacker,
                    defender = defender,
                    rawBaseDamage = proj.damage,
                    knockbackSpeed = 3.2f,
                    heavyImpact = true,
                    causesKnockdown = false
                )
                // Heavy projectiles also chip nearby pillars on detonation
                damageNearbyPillars(proj.x, radius = 1.5f, integrityDamage = 35f, attacker = attacker, defender = defender)
                iter.remove()
            }
        }
    }

    private fun applyHitToTarget(
        attacker: FighterActor,
        defender: FighterActor,
        rawBaseDamage: Float,
        knockbackSpeed: Float,
        heavyImpact: Boolean,
        causesKnockdown: Boolean
    ) {
        if (defender.isInvulnerableDodge || defender.hp <= 0f) return

        // Tactical bonus: Shadow Tiger striking out of smoke veil deals 22% ambush bonus!
        val smokeAmbushMultiplier = if (attacker.stealthSmokeCloakTimer > 0f && attacker.profile.id == FighterId.SHADOW_TIGER) 1.22f else 1.0f
        // Tactical bonus: Enemy standing on shattered floor takes +15% stagger damage
        val crackedFloorMultiplier = if (defender.slowedByFloorCrackTimer > 0f) 1.15f else 1.0f

        var computedDamage = rawBaseDamage * attacker.profile.attackPower * defender.profile.defenseFactor * smokeAmbushMultiplier * crackedFloorMultiplier
        val pushDir = if (defender.x >= attacker.x) 1f else -1f

        if (defender.isBlockingActive) {
            computedDamage *= 0.22f // Blocking reduces 78% of damage
            defender.hp = max(0f, defender.hp - computedDamage)
            defender.vx = pushDir * (knockbackSpeed * 0.38f)
            attacker.ultimateEnergy = min(100f, attacker.ultimateEnergy + 5f)
            defender.ultimateEnergy = min(100f, defender.ultimateEnergy + 7f)
            spawnBurstParticles(defender.x - pushDir * 0.4f, defender.y + 1.15f, 0f, 0xFF00E5FF.toInt(), 10, 4.0f)
            audio?.play(SoundCue.BLOCK_CLANG)
            if (attacker.isPlayer) totalDamageDealt += computedDamage
            return
        }

        defender.hp = max(0f, defender.hp - computedDamage)
        defender.hitFlashTimer = 0.20f

        // Combo tracking
        attacker.comboCount += 1
        attacker.comboDecayTimer = 2.1f
        if (attacker.isPlayer) {
            totalDamageDealt += computedDamage
            maxPlayerCombo = max(maxPlayerCombo, attacker.comboCount)
        }

        // Ultimate energy gain on landed hits AND when taking damage
        attacker.ultimateEnergy = min(100f, attacker.ultimateEnergy + if (heavyImpact) 16f else 11f)
        defender.ultimateEnergy = min(100f, defender.ultimateEnergy + if (heavyImpact) 13f else 9f)

        // Hit reaction vs Poise / Super-Armor (e.g. Titan Rhino & Boss resist light interruption)
        val interruptsTarget = causesKnockdown ||
            (defender.state != ActionState.ULTIMATE_CAST && rng.nextFloat() > defender.profile.poiseArmor * (if (heavyImpact) 0.35f else 0.85f))

        if (interruptsTarget && defender.state != ActionState.ULTIMATE_CAST) {
            defender.currentMove = MoveType.NONE
            if (causesKnockdown) {
                defender.state = ActionState.KNOCKDOWN
                defender.stateTimer = 0f
                defender.vx = pushDir * knockbackSpeed
                defender.vy = 4.2f
                defender.y = max(defender.y, 0.04f)
            } else {
                defender.state = ActionState.HIT_STUN
                defender.stateTimer = 0f
                defender.vx = pushDir * knockbackSpeed
            }
        } else {
            defender.vx += pushDir * (knockbackSpeed * 0.3f)
        }

        // Knocking defender backward into an intact pillar damages the pillar!
        if (heavyImpact) {
            damageNearbyPillars(defender.x, radius = 1.15f, integrityDamage = 50f, attacker = attacker, defender = defender)
        }

        spawnBurstParticles(
            x = (attacker.x + defender.x) * 0.5f,
            y = defender.y + 1.15f,
            z = 0f,
            colorHex = if (heavyImpact) attacker.profile.glowHex else 0xFFFFEA00.toInt(),
            count = if (heavyImpact) 18 else 10,
            speed = if (heavyImpact) 5.8f else 3.8f
        )

        if (heavyImpact) {
            triggerCameraShake(0.38f)
            audio?.play(SoundCue.HEAVY_HIT)
        } else {
            triggerCameraShake(0.14f)
            audio?.play(SoundCue.LIGHT_HIT)
        }
    }

    private fun resolveFighterBodySeparation() {
        val minSep = if (opponent.profile.isBoss) 1.25f else 1.05f
        val dx = opponent.x - player.x
        val verticalOverlap = abs(opponent.y - player.y) < 1.35f
        if (verticalOverlap && abs(dx) < minSep) {
            if (player.isInvulnerableDodge || opponent.isInvulnerableDodge) return
            val push = (minSep - abs(dx)) * 0.5f
            val sign = if (dx >= 0f) 1f else -1f
            player.x = (player.x - sign * push).coerceIn(-arenaHalfWidth + 0.65f, arenaHalfWidth - 0.65f)
            opponent.x = (opponent.x + sign * push).coerceIn(-arenaHalfWidth + 0.65f, arenaHalfWidth - 0.65f)
        }
    }

    private fun updateGravityOnly(actor: FighterActor, dt: Float) {
        if (!actor.isGrounded || actor.vy > 0f) {
            actor.y += actor.vy * dt
            actor.vy += gravity * dt
            if (actor.y <= 0f) {
                actor.y = 0f
                actor.vy = 0f
            }
        }
    }

    private fun updatePillars(dt: Float) {
        for (p in pillars) {
            p.hitFlash = max(0f, p.hitFlash - dt)
            if (p.isBroken && p.collapseAnim < 1f) {
                p.collapseAnim = min(1f, p.collapseAnim + dt * 2.4f)
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val pt = iter.next()
            pt.life -= dt
            if (pt.life <= 0f) {
                iter.remove()
                continue
            }
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.z += pt.vz * dt
            pt.vy += pt.gravity * dt
            if (pt.y < 0.02f) {
                pt.y = 0.02f
                pt.vy = -pt.vy * 0.25f
                pt.vx *= 0.7f
            }
        }
    }

    private fun spawnBurstParticles(x: Float, y: Float, z: Float, colorHex: Int, count: Int, speed: Float) {
        repeat(count) {
            val angle = rng.nextFloat() * 6.28318f
            val elev = (rng.nextFloat() - 0.25f) * speed
            val radial = (0.3f + rng.nextFloat() * 0.7f) * speed
            addParticle(
                Particle3D(
                    x = x,
                    y = y,
                    z = z,
                    vx = cos(angle) * radial,
                    vy = elev + 1.5f,
                    vz = sin(angle) * radial * 0.65f,
                    size = 0.09f + rng.nextFloat() * 0.11f,
                    colorHex = colorHex,
                    life = 0.35f + rng.nextFloat() * 0.30f,
                    maxLife = 0.65f
                )
            )
        }
    }

    private fun spawnDustPuff(x: Float, y: Float, z: Float, count: Int) {
        repeat(count) {
            val angle = rng.nextFloat() * 6.28318f
            addParticle(
                Particle3D(
                    x = x + (rng.nextFloat() - 0.5f) * 0.45f,
                    y = y,
                    z = z + (rng.nextFloat() - 0.5f) * 0.45f,
                    vx = cos(angle) * (0.6f + rng.nextFloat() * 1.4f),
                    vy = 0.7f + rng.nextFloat() * 1.5f,
                    vz = sin(angle) * (0.6f + rng.nextFloat() * 1.1f),
                    size = 0.16f + rng.nextFloat() * 0.14f,
                    colorHex = 0xFF78909C.toInt(),
                    life = 0.48f + rng.nextFloat() * 0.32f,
                    maxLife = 0.80f,
                    gravity = -1.2f
                )
            )
        }
    }

    private fun addParticle(particle: Particle3D) {
        // Hard cap 140 particles for locked 60 FPS on mid-range Android GPUs
        if (particles.size >= 140) {
            particles.removeAt(0)
        }
        particles.add(particle)
    }

    private fun concludeMatch() {
        battleFinished = true
        val playerPct = player.hp / player.profile.maxHealth
        val oppPct = opponent.hp / opponent.profile.maxHealth
        playerWonMatch = when {
            opponent.hp <= 0f -> true
            player.hp <= 0f -> false
            else -> playerPct >= oppPct
        }

        if (playerWonMatch) {
            player.state = ActionState.VICTORY
            player.stateTimer = 0f
            opponent.state = ActionState.DEFEAT
            opponent.stateTimer = 0f
            bannerText = if (opponent.profile.isBoss) {
                "OVERLORD VANQUISHED — CHAMPION!"
            } else {
                "K.O. — ${player.profile.displayName} WINS!"
            }
            bannerTimer = 10f
            audio?.play(SoundCue.VICTORY_FANFARE)
        } else {
            player.state = ActionState.DEFEAT
            player.stateTimer = 0f
            opponent.state = ActionState.VICTORY
            opponent.stateTimer = 0f
            bannerText = "DEFEATED — RISE AGAIN WARRIOR!"
            bannerTimer = 10f
            audio?.play(SoundCue.DEFEAT_THUD)
        }
    }

    private fun triggerCameraShake(amount: Float) {
        if (cameraShakeEnabled) {
            cameraShakeIntensity = max(cameraShakeIntensity, amount)
        }
    }

    private fun triggerArenaFlash(colorHex: Int, intensity: Float) {
        arenaFlashColorHex = colorHex
        arenaFlashIntensity = max(arenaFlashIntensity, intensity)
    }

    fun copyRenderState(
        outPillars: MutableList<ArenaPillar>,
        outZones: MutableList<ArenaZoneEffect>,
        outProjectiles: MutableList<ProjectileEntity>,
        outParticles: MutableList<Particle3D>
    ): Pair<FighterRenderSnapshot, FighterRenderSnapshot> {
        synchronized(lock) {
            outPillars.clear()
            for (p in pillars) outPillars.add(p.copy())

            outZones.clear()
            for (z in zoneEffects) outZones.add(z.copy())

            outProjectiles.clear()
            for (pr in projectiles) outProjectiles.add(pr.copy())

            outParticles.clear()
            for (pt in particles) outParticles.add(pt.copy())

            return Pair(player.toRenderSnapshot(), opponent.toRenderSnapshot())
        }
    }

    private fun FighterActor.toRenderSnapshot() = FighterRenderSnapshot(
        profile = profile,
        isPlayer = isPlayer,
        x = x,
        y = y,
        z = z,
        facingSign = facingSign,
        state = state,
        currentMove = currentMove,
        stateTimer = stateTimer,
        moveDuration = moveDuration,
        animTime = animTime,
        hitFlash = hitFlashTimer > 0f,
        specialGlow = specialGlowTimer > 0f || state == ActionState.ULTIMATE_CAST,
        smokeCloaked = stealthSmokeCloakTimer > 0f
    )

    private fun buildHudSnapshot(): CombatHudSnapshot {
        val tacticalMsg = when {
            player.stealthSmokeCloakTimer > 0f -> "SMOKE VEIL ACTIVE: ENEMY VISION OBSCURED"
            opponent.slowedByFloorCrackTimer > 0f -> "SEISMIC CRACK: OPPONENT SLOWED & VULNERABLE"
            player.slowedByFloorCrackTimer > 0f -> "SHATTERED FLOOR: MOVEMENT SLOWED"
            zoneEffects.any { it.type == ArenaHazardType.SCORCHED_FLAME_ZONE } -> "SCORCHED ARENA: FLAME FISSURES ACTIVE"
            else -> ""
        }
        return CombatHudSnapshot(
            stageNumber = stageNumber,
            playerProfile = player.profile,
            opponentProfile = opponent.profile,
            playerHp = player.hp,
            playerMaxHp = player.profile.maxHealth,
            opponentHp = opponent.hp,
            opponentMaxHp = opponent.profile.maxHealth,
            playerUltimate = player.ultimateEnergy,
            opponentUltimate = opponent.ultimateEnergy,
            roundTimerSeconds = roundTimer.toInt(),
            playerCombo = player.comboCount,
            opponentCombo = opponent.comboCount,
            bannerAnnouncement = bannerText,
            bossWarningText = bossWarningText,
            bossPhase = bossPhase,
            activeTacticalStatus = tacticalMsg,
            opponentObscuredBySmoke = player.stealthSmokeCloakTimer > 0f,
            brokenPillarsCount = pillars.count { it.isBroken },
            totalPillarsCount = pillars.size,
            isIntroPlaying = introTimer > 0f,
            isBattleOver = battleFinished,
            didPlayerWin = playerWonMatch,
            isPaused = isPaused,
            damageDealtTotal = totalDamageDealt.toInt(),
            maxComboAchieved = maxPlayerCombo,
            arenaInteractionsTriggered = arenaInteractionsCount,
            battleElapsedSeconds = elapsedMatchTime.toInt()
        )
    }

    private object FighterProfilePlaceholder {
        fun defaultPlayer() = com.example.game.model.FighterRoster.IRON_BUFFALO
        fun defaultOpponent() = com.example.game.model.FighterRoster.TITAN_RHINO
    }
}

data class FighterRenderSnapshot(
    val profile: FighterProfile,
    val isPlayer: Boolean,
    val x: Float,
    val y: Float,
    val z: Float,
    val facingSign: Float,
    val state: ActionState,
    val currentMove: MoveType,
    val stateTimer: Float,
    val moveDuration: Float,
    val animTime: Float,
    val hitFlash: Boolean,
    val specialGlow: Boolean,
    val smokeCloaked: Boolean
)
