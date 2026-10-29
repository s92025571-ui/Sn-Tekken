package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BuffaloAmber
import com.example.ui.theme.DragonInferno
import com.example.ui.theme.OverlordCrimson
import com.example.ui.theme.RhinoSlate
import com.example.ui.theme.TigerAzure

enum class FighterId {
    IRON_BUFFALO,
    TITAN_RHINO,
    SHADOW_TIGER,
    INFERNO_DRAGON,
    SHADOW_DRAGON_OVERLORD
}

data class FighterAbilityInfo(
    val name: String,
    val description: String
)

data class FighterProfile(
    val id: FighterId,
    val displayName: String,
    val animalIdentity: String,
    val fightingStyle: String,
    val isPlayable: Boolean,
    val isBoss: Boolean = false,
    val maxHealth: Float,
    val moveSpeed: Float,       // units per sec
    val attackPower: Float,     // damage multiplier
    val defenseFactor: Float,   // incoming damage multiplier (lower = stronger defense)
    val jumpVelocity: Float,
    val attackReach: Float,
    val projectileSpeed: Float,
    val poiseArmor: Float,      // resistance to hitstun interruption
    val statSpeedRating: Int,   // 1..100 for UI bars
    val statPowerRating: Int,
    val statDefenseRating: Int,
    val statRangeRating: Int,
    val accentColor: Color,
    val primaryHex: Int,
    val secondaryHex: Int,
    val glowHex: Int,
    val ultimateName: String,
    val ultimateDescription: String,
    val arenaTacticalAdvantage: String,
    val abilities: List<FighterAbilityInfo>
)

object FighterRoster {
    val IRON_BUFFALO = FighterProfile(
        id = FighterId.IRON_BUFFALO,
        displayName = "IRON BUFFALO",
        animalIdentity = "Water Buffalo Heavyweight",
        fightingStyle = "Heavyweight Brawler",
        isPlayable = true,
        maxHealth = 100f,
        moveSpeed = 3.9f,
        attackPower = 1.32f,
        defenseFactor = 0.86f,
        jumpVelocity = 7.4f,
        attackReach = 2.05f,
        projectileSpeed = 8.4f,
        poiseArmor = 0.45f,
        statSpeedRating = 48,
        statPowerRating = 96,
        statDefenseRating = 84,
        statRangeRating = 68,
        accentColor = BuffaloAmber,
        primaryHex = 0xFF282A30.toInt(),
        secondaryHex = 0xFF4B4E58.toInt(),
        glowHex = 0xFFFF7A00.toInt(),
        ultimateName = "BUFFALO APOCALYPSE",
        ultimateDescription = "Charges with blazing horns, pummels the foe, and shatters the arena floor in a seismic blast.",
        arenaTacticalAdvantage = "Seismic Floor Shatter: Heavy & Ultimate stomps crack the octagonal floor, creating jagged shock-zones that slow and stagger grounded foes.",
        abilities = listOf(
            FighterAbilityInfo("Horn-Assisted Punch", "Crushing forward hook reinforced by curved buffalo horns."),
            FighterAbilityInfo("Ground-Shaking Stomp", "Shatters arena floor tiles and kicks up blinding dust shockwaves."),
            FighterAbilityInfo("Charging Stampede", "Relentless shoulder-and-horn rush that smashes through arena pillars."),
            FighterAbilityInfo("Compressed Shockwave", "Dark-orange seismic energy wave hurled across the stage.")
        )
    )

    val TITAN_RHINO = FighterProfile(
        id = FighterId.TITAN_RHINO,
        displayName = "TITAN RHINO",
        animalIdentity = "Armored Rhinoceros",
        fightingStyle = "Defensive Tank & Heavy Striker",
        isPlayable = true,
        maxHealth = 100f,
        moveSpeed = 4.1f,
        attackPower = 1.20f,
        defenseFactor = 0.74f,
        jumpVelocity = 7.1f,
        attackReach = 1.98f,
        projectileSpeed = 7.9f,
        poiseArmor = 0.72f,
        statSpeedRating = 52,
        statPowerRating = 86,
        statDefenseRating = 98,
        statRangeRating = 64,
        accentColor = RhinoSlate,
        primaryHex = 0xFF5D6670.toInt(),
        secondaryHex = 0xFF8C97A4.toInt(),
        glowHex = 0xFFB0BEC5.toInt(),
        ultimateName = "RHINO EARTHBREAKER",
        ultimateDescription = "Armored juggernaut charge into a tectonic shockwave and sky-piercing nasal horn strike.",
        arenaTacticalAdvantage = "Pillar Shrapnel Bash: Smashing into cyber-pillars hurls stone & neon shrapnel into the opponent for bonus stun.",
        abilities = listOf(
            FighterAbilityInfo("Rhino Horn Uppercut", "Anti-air nasal horn thrust with super-armor frames."),
            FighterAbilityInfo("Armored Body Charge", "Unstoppable thick-plated rush that shatters arena structures."),
            FighterAbilityInfo("Tectonic Ground Slam", "Two-fisted slam sending rock splinters upward."),
            FighterAbilityInfo("Compressed Rock Blast", "Dense gray boulder-energy projectile with high knockback.")
        )
    )

    val SHADOW_TIGER = FighterProfile(
        id = FighterId.SHADOW_TIGER,
        displayName = "SHADOW TIGER",
        animalIdentity = "Bengal Tiger Martial Artist",
        fightingStyle = "Fast Agile Combo Fighter",
        isPlayable = true,
        maxHealth = 100f,
        moveSpeed = 6.2f,
        attackPower = 0.96f,
        defenseFactor = 1.02f,
        jumpVelocity = 9.4f,
        attackReach = 1.85f,
        projectileSpeed = 11.6f,
        poiseArmor = 0.15f,
        statSpeedRating = 98,
        statPowerRating = 76,
        statDefenseRating = 62,
        statRangeRating = 74,
        accentColor = TigerAzure,
        primaryHex = 0xFFE66A12.toInt(),
        secondaryHex = 0xFFF5F5F5.toInt(),
        glowHex = 0xFF00C8FF.toInt(),
        ultimateName = "PHANTOM TIGER FURY",
        ultimateDescription = "Blinding flurry of azure-lit claw slashes, phantom cross-dash, and colossal tiger-spirit finisher.",
        arenaTacticalAdvantage = "Smoke Veil Ambush: Kicking up dust clouds or smoke from broken pillars cloaks Shadow Tiger, obscuring enemy tracking and boosting crit damage.",
        abilities = listOf(
            FighterAbilityInfo("Rapid Claw Strikes", "Lightning-fast multi-hit Bengal claw combo."),
            FighterAbilityInfo("Cyclone Spinning Kick", "Acrobatic tail-whipping roundhouse that kicks up arena dust."),
            FighterAbilityInfo("Shadow Pounce", "Low-profile predatory leap through smoke clouds."),
            FighterAbilityInfo("Azure Claw Crescent", "High-velocity blue energy slash shaped like twin tiger claws.")
        )
    )

    val INFERNO_DRAGON = FighterProfile(
        id = FighterId.INFERNO_DRAGON,
        displayName = "INFERNO DRAGON",
        animalIdentity = "Bipedal Elder Fire Dragon",
        fightingStyle = "Balanced Aerial & Flame Striker",
        isPlayable = true,
        maxHealth = 100f,
        moveSpeed = 5.2f,
        attackPower = 1.10f,
        defenseFactor = 0.92f,
        jumpVelocity = 9.0f,
        attackReach = 2.10f,
        projectileSpeed = 10.8f,
        poiseArmor = 0.30f,
        statSpeedRating = 80,
        statPowerRating = 84,
        statDefenseRating = 76,
        statRangeRating = 94,
        accentColor = DragonInferno,
        primaryHex = 0xFFA31818.toInt(),
        secondaryHex = 0xFFD96B16.toInt(),
        glowHex = 0xFFFF4500.toInt(),
        ultimateName = "DRAGON INFERNO",
        ultimateDescription = "Unleashes searing dragonfire breath, launches a solar core fireball, and detonates a draconic blast.",
        arenaTacticalAdvantage = "Scorched Fissure Ignition: Fire attacks ignite cracked arena floor plates and broken pillar cores into lingering flame hazards.",
        abilities = listOf(
            FighterAbilityInfo("Dragon Claw Combo", "Rending twin-talon slashes backed by wing gusts."),
            FighterAbilityInfo("Searing Flame Breath", "Cone of draconic fire that ignites shattered floor fissures."),
            FighterAbilityInfo("Blazing Wing Kick", "Aerial spinning talon strike propelled by folded dragon wings."),
            FighterAbilityInfo("Crimson Fireball", "Explosive red-orange draconic plasma orb.")
        )
    )

    val SHADOW_DRAGON_OVERLORD = FighterProfile(
        id = FighterId.SHADOW_DRAGON_OVERLORD,
        displayName = "SHADOW DRAGON OVERLORD",
        animalIdentity = "Abyssal Dragon Emperor (BOSS)",
        fightingStyle = "Cataclysmic Two-Phase Boss",
        isPlayable = false,
        isBoss = true,
        maxHealth = 300f,
        moveSpeed = 4.9f,
        attackPower = 1.28f,
        defenseFactor = 0.85f,
        jumpVelocity = 8.8f,
        attackReach = 2.55f,
        projectileSpeed = 11.2f,
        poiseArmor = 0.65f,
        statSpeedRating = 85,
        statPowerRating = 99,
        statDefenseRating = 95,
        statRangeRating = 98,
        accentColor = OverlordCrimson,
        primaryHex = 0xFF140D18.toInt(),
        secondaryHex = 0xFF7A0918.toInt(),
        glowHex = 0xFFFF0033.toInt(),
        ultimateName = "ABYSSAL CATACLYSM",
        ultimateDescription = "Unleashes a dark-energy shockwave that shatters surrounding pillars and scorches the arena.",
        arenaTacticalAdvantage = "Abyssal Shockwave: Phase 2 dark energy detonates intact pillars and sends shockwaves across the platform.",
        abilities = listOf(
            FighterAbilityInfo("Dark Fireball", "Corrupted crimson-black plasma sphere."),
            FighterAbilityInfo("Abyssal Claw Combo", "Crushing three-hit heavy talon assault."),
            FighterAbilityInfo("Cataclysm Ground Slam & Tail Sweep", "Shatters arena stone and sweeps close-range challengers."),
            FighterAbilityInfo("Dark-Energy Shockwave", "Telegraphed arena-wide crimson blast — dodge or block to survive!")
        )
    )

    val playableFighters: List<FighterProfile> = listOf(
        IRON_BUFFALO,
        TITAN_RHINO,
        SHADOW_TIGER,
        INFERNO_DRAGON
    )

    fun byId(id: FighterId): FighterProfile = when (id) {
        FighterId.IRON_BUFFALO -> IRON_BUFFALO
        FighterId.TITAN_RHINO -> TITAN_RHINO
        FighterId.SHADOW_TIGER -> SHADOW_TIGER
        FighterId.INFERNO_DRAGON -> INFERNO_DRAGON
        FighterId.SHADOW_DRAGON_OVERLORD -> SHADOW_DRAGON_OVERLORD
    }

    fun buildCampaignOpponents(playerFighter: FighterId): List<FighterProfile> {
        val others = playableFighters.filter { it.id != playerFighter }
        // Deterministic yet varied arcade bracket: pick 2 distinct animal warriors + Boss Battle 3
        val battle1 = others[0]
        val battle2 = others[1]
        return listOf(battle1, battle2, SHADOW_DRAGON_OVERLORD)
    }
}
