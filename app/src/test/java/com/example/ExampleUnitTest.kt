package com.example

import com.example.game.engine.CombatEngine
import com.example.game.model.AiDifficulty
import com.example.game.model.ArenaHazardType
import com.example.game.model.FighterId
import com.example.game.model.FighterRoster
import com.example.game.model.MoveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `four playable animal warriors and boss have distinct stats and campaign order`() {
        assertEquals(4, FighterRoster.playableFighters.size)
        val ids = FighterRoster.playableFighters.map { it.id }.toSet()
        assertTrue(ids.contains(FighterId.IRON_BUFFALO))
        assertTrue(ids.contains(FighterId.TITAN_RHINO))
        assertTrue(ids.contains(FighterId.SHADOW_TIGER))
        assertTrue(ids.contains(FighterId.INFERNO_DRAGON))

        // Shadow Tiger is fastest, Iron Buffalo has highest attack power, Titan Rhino has strongest defense
        assertTrue(FighterRoster.SHADOW_TIGER.moveSpeed > FighterRoster.IRON_BUFFALO.moveSpeed)
        assertTrue(FighterRoster.IRON_BUFFALO.attackPower > FighterRoster.SHADOW_TIGER.attackPower)
        assertTrue(FighterRoster.TITAN_RHINO.defenseFactor < FighterRoster.SHADOW_TIGER.defenseFactor)

        // Campaign progression always puts Shadow Dragon Overlord (300 HP) on Stage 3
        val bracket = FighterRoster.buildCampaignOpponents(FighterId.IRON_BUFFALO)
        assertEquals(3, bracket.size)
        assertEquals(FighterId.SHADOW_DRAGON_OVERLORD, bracket[2].id)
        assertEquals(300f, bracket[2].maxHealth, 0.01f)
    }

    @Test
    fun `iron buffalo heavy stomp shatters arena floor and damages opponent in close range`() {
        val engine = CombatEngine(audio = null)
        engine.startBattle(
            playerProfile = FighterRoster.IRON_BUFFALO,
            opponentProfile = FighterRoster.TITAN_RHINO,
            stage = 1,
            aiDiff = AiDifficulty.CHALLENGER
        )
        // Advance past cinematic intro (2.0s)
        repeat(50) { engine.step(0.05f) }

        // Place fighters within melee range near Pillar #2 (-2.1f)
        engine.player.x = -2.2f
        engine.opponent.x = -1.0f

        val initialOppHp = engine.opponent.hp
        engine.requestPlayerAttack(MoveType.HEAVY)
        repeat(15) { engine.step(0.04f) }

        assertTrue("Opponent should take stomp damage", engine.opponent.hp < initialOppHp)
        assertTrue(
            "Iron Buffalo stomp should create SHATTERED_FLOOR tactical zone",
            engine.zoneEffects.any { it.type == ArenaHazardType.SHATTERED_FLOOR }
        )
    }

    @Test
    fun `shadow tiger dodge and kick generate tactical smoke cloud`() {
        val engine = CombatEngine(audio = null)
        engine.startBattle(
            playerProfile = FighterRoster.SHADOW_TIGER,
            opponentProfile = FighterRoster.INFERNO_DRAGON,
            stage = 1,
            aiDiff = AiDifficulty.CHALLENGER
        )
        repeat(50) { engine.step(0.05f) }

        engine.requestPlayerDodge()
        engine.step(0.05f)

        assertTrue(
            "Shadow Tiger dodge should kick up DUST_SMOKE_CLOUD",
            engine.zoneEffects.any { it.type == ArenaHazardType.DUST_SMOKE_CLOUD }
        )
        assertTrue("Shadow Tiger should gain smoke stealth cloak", engine.player.stealthSmokeCloakTimer > 0f)
    }

    @Test
    fun `projectiles travel across arena and collide with opponent`() {
        val engine = CombatEngine(audio = null)
        engine.startBattle(
            playerProfile = FighterRoster.INFERNO_DRAGON,
            opponentProfile = FighterRoster.IRON_BUFFALO,
            stage = 2,
            aiDiff = AiDifficulty.CHALLENGER
        )
        repeat(50) { engine.step(0.05f) }

        engine.player.x = -2.5f
        engine.opponent.x = 1.5f
        val startHp = engine.opponent.hp

        engine.requestPlayerAttack(MoveType.PROJECTILE)
        // Step through startup + projectile travel
        repeat(30) { engine.step(0.03f) }

        assertTrue("Fireball projectile should travel and reduce opponent HP", engine.opponent.hp < startHp)
    }

    @Test
    fun `boss transitions to phase 2 below 50 percent health and shatters nearby pillar`() {
        val engine = CombatEngine(audio = null)
        engine.startBattle(
            playerProfile = FighterRoster.IRON_BUFFALO,
            opponentProfile = FighterRoster.SHADOW_DRAGON_OVERLORD,
            stage = 3,
            aiDiff = AiDifficulty.WARRIOR
        )
        repeat(50) { engine.step(0.05f) }

        engine.opponent.x = 2.1f
        engine.opponent.hp = 140f // < 50% of 300 HP
        val hud = engine.step(0.04f)

        assertEquals(2, hud.bossPhase)
        assertTrue(
            "Boss phase 2 transition damages nearby arena pillar",
            engine.pillars.any { it.integrity < 100f }
        )
    }
}
