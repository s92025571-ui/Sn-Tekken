package com.example.game.model

enum class MoveType {
    NONE,
    PUNCH,          // Light fast punch
    KICK,           // Medium kick + dust kickup on arena
    HEAVY,          // Heavy strike / stomp / uppercut — breaks pillars & cracks floor
    SPECIAL_MELEE,  // Signature animal rush/claw/breath
    PROJECTILE,     // Ranged supernatural energy projectile
    ULTIMATE,       // Full-meter cinematic Ultimate Power
    BOSS_TAIL_SWEEP,
    BOSS_FLAME_BREATH,
    BOSS_DARK_SHOCKWAVE
}

enum class ActionState {
    INTRO,
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    RUN,
    JUMP,
    ATTACK_STARTUP,
    ATTACK_ACTIVE,
    ATTACK_RECOVERY,
    BLOCK,
    DODGE,
    HIT_STUN,
    KNOCKDOWN,
    ULTIMATE_CAST,
    VICTORY,
    DEFEAT
}

enum class ArenaHazardType {
    DESTRUCTIBLE_PILLAR, // 4 neon cyber-stone pillars around the stage edge/combat lane
    SHATTERED_FLOOR,     // Created by Iron Buffalo stomp / Titan Rhino slam / Boss slam
    DUST_SMOKE_CLOUD,    // Created by pillar collapse or Shadow Tiger / heavy kicks; obscures AI & grants stealth evade/crit
    SCORCHED_FLAME_ZONE  // Created when Inferno Dragon or Boss ignites cracked floor or broken pillar core
}

data class ArenaPillar(
    val id: Int,
    val x: Float,
    val z: Float,
    val neonHex: Int,
    var integrity: Float = 100f, // 100 -> intact, <=50 -> cracked, <=0 -> shattered!
    var collapseAnim: Float = 0f, // 0..1 collapse progress once broken
    var hitFlash: Float = 0f
) {
    val isBroken: Boolean get() = integrity <= 0f
}

data class ArenaZoneEffect(
    val id: Int,
    val type: ArenaHazardType,
    val x: Float,
    val z: Float,
    val radius: Float,
    var remainingTime: Float,
    val maxTime: Float,
    val ownerIsPlayer: Boolean,
    var tickTimer: Float = 0f
)

data class ProjectileEntity(
    val id: Int,
    val ownerIsPlayer: Boolean,
    val sourceFighter: FighterId,
    var x: Float,
    var y: Float,
    var z: Float,
    val vx: Float,
    val radius: Float,
    val damage: Float,
    val colorHex: Int,
    val secondaryHex: Int,
    var life: Float = 3.0f,
    var active: Boolean = true
)

data class Particle3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var size: Float,
    var colorHex: Int,
    var life: Float,
    var maxLife: Float,
    var gravity: Float = -9.5f
)

data class CombatHudSnapshot(
    val stageNumber: Int = 1, // 1, 2, or 3 (Boss)
    val playerProfile: FighterProfile = FighterRoster.IRON_BUFFALO,
    val opponentProfile: FighterProfile = FighterRoster.SHADOW_TIGER,
    val playerHp: Float = 100f,
    val playerMaxHp: Float = 100f,
    val opponentHp: Float = 100f,
    val opponentMaxHp: Float = 100f,
    val playerUltimate: Float = 0f, // 0..100
    val opponentUltimate: Float = 0f,
    val roundTimerSeconds: Int = 99,
    val playerCombo: Int = 0,
    val opponentCombo: Int = 0,
    val bannerAnnouncement: String = "",
    val bossWarningText: String = "",
    val bossPhase: Int = 1,
    val activeTacticalStatus: String = "",
    val opponentObscuredBySmoke: Boolean = false,
    val brokenPillarsCount: Int = 0,
    val totalPillarsCount: Int = 4,
    val isIntroPlaying: Boolean = true,
    val isBattleOver: Boolean = false,
    val didPlayerWin: Boolean = false,
    val isPaused: Boolean = false,
    val damageDealtTotal: Int = 0,
    val maxComboAchieved: Int = 0,
    val arenaInteractionsTriggered: Int = 0,
    val battleElapsedSeconds: Int = 0
)

enum class AppScreen {
    MAIN_MENU,
    CHARACTER_SELECT,
    BATTLE_ARENA,
    CAMPAIGN_VICTORY,
    SETTINGS
}

enum class AiDifficulty(val label: String, val reactionScale: Float, val aggressionScale: Float) {
    CHALLENGER("Challenger", 0.85f, 0.80f),
    WARRIOR("Warrior", 1.0f, 1.0f),
    OVERLORD("Master Beast", 1.22f, 1.25f)
}
