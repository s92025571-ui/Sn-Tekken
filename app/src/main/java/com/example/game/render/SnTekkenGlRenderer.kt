package com.example.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.CombatEngine
import com.example.game.engine.FighterRenderSnapshot
import com.example.game.model.ActionState
import com.example.game.model.ArenaHazardType
import com.example.game.model.ArenaPillar
import com.example.game.model.ArenaZoneEffect
import com.example.game.model.CombatHudSnapshot
import com.example.game.model.FighterId
import com.example.game.model.FighterProfile
import com.example.game.model.MoveType
import com.example.game.model.Particle3D
import com.example.game.model.ProjectileEntity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class SnTekkenGlRenderer(
    private val combatEngine: CombatEngine?,
    private val previewFighterProvider: (() -> FighterProfile)? = null,
    private val onHudUpdated: ((CombatHudSnapshot) -> Unit)? = null
) : GLSurfaceView.Renderer, PartDrawCallback {

    private var shaderProgram = 0
    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var uMvpMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uColorLoc = 0
    private var uEmissiveLoc = 0
    private var uLightDirLoc = 0
    private var uFogColorLoc = 0
    private var uCameraPosLoc = 0

    private lateinit var boxMesh: MeshBuffer3D
    private lateinit var coneMesh: MeshBuffer3D
    private lateinit var prismMesh: MeshBuffer3D
    private lateinit var sphereMesh: MeshBuffer3D
    private lateinit var rigRenderer: AnimalWarriorRigRenderer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private val renderPillars = ArrayList<ArenaPillar>(8)
    private val renderZones = ArrayList<ArenaZoneEffect>(8)
    private val renderProjectiles = ArrayList<ProjectileEntity>(16)
    private val renderParticles = ArrayList<Particle3D>(160)

    private var lastFrameNano = 0L
    private var totalElapsedSec = 0f
    private var hudEmitAccumulator = 0f

    private var camEyeX = 0f
    private var camEyeY = 2.6f
    private var camEyeZ = 8.5f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.03f, 0.045f, 0.085f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shaderProgram = buildShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(shaderProgram, "aNormal")
        uMvpMatrixLoc = GLES20.glGetUniformLocation(shaderProgram, "uMvpMatrix")
        uModelMatrixLoc = GLES20.glGetUniformLocation(shaderProgram, "uModelMatrix")
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uColor")
        uEmissiveLoc = GLES20.glGetUniformLocation(shaderProgram, "uEmissive")
        uLightDirLoc = GLES20.glGetUniformLocation(shaderProgram, "uLightDir")
        uFogColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uFogColor")
        uCameraPosLoc = GLES20.glGetUniformLocation(shaderProgram, "uCameraPos")

        boxMesh = ProceduralGeometry3D.createUnitBox()
        coneMesh = ProceduralGeometry3D.createCone(10)
        prismMesh = ProceduralGeometry3D.createPrism(8)
        sphereMesh = ProceduralGeometry3D.createSphere(8, 12)
        rigRenderer = AnimalWarriorRigRenderer(boxMesh, coneMesh, prismMesh, sphereMesh)

        lastFrameNano = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
        val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 44f, ratio, 0.5f, 65f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastFrameNano = now
        totalElapsedSec += dt

        val engine = combatEngine
        if (engine != null) {
            val hud = engine.step(dt)
            hudEmitAccumulator += dt
            if (hudEmitAccumulator >= 0.032f || hud.isBattleOver) {
                hudEmitAccumulator = 0f
                onHudUpdated?.invoke(hud)
            }
            renderArenaBattleFrame(engine)
        } else {
            val previewFighter = previewFighterProvider?.invoke()
            if (previewFighter != null) {
                renderCharacterSelectTurntable(previewFighter)
            }
        }
    }

    private fun renderCharacterSelectTurntable(profile: FighterProfile) {
        GLES20.glClearColor(0.035f, 0.05f, 0.09f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        camEyeX = 0f
        camEyeY = 1.65f
        camEyeZ = 4.15f
        Matrix.setLookAtM(
            viewMatrix, 0,
            camEyeX, camEyeY, camEyeZ,
            0f, 1.15f, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(shaderProgram)
        GLES20.glUniform3f(uLightDirLoc, 0.35f, 0.85f, 0.55f)
        GLES20.glUniform3f(uFogColorLoc, 0.035f, 0.05f, 0.09f)
        GLES20.glUniform3f(uCameraPosLoc, camEyeX, camEyeY, camEyeZ)

        // Glowing Beast Pedestal
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0f, -0.14f, 0f)
        Matrix.scaleM(tempMatrix, 0, 2.3f, 0.28f, 2.3f)
        drawPart(prismMesh, tempMatrix, 0xFF141B2D.toInt(), emissive = 0.1f)

        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0f, 0.01f, 0f)
        Matrix.scaleM(tempMatrix, 0, 2.42f, 0.03f, 2.42f)
        drawPart(prismMesh, tempMatrix, profile.glowHex, emissive = 0.90f)

        val snap = FighterRenderSnapshot(
            profile = profile,
            isPlayer = true,
            x = 0f,
            y = 0f,
            z = 0f,
            facingSign = 1f,
            state = ActionState.IDLE,
            currentMove = MoveType.NONE,
            stateTimer = 0f,
            moveDuration = 1f,
            animTime = totalElapsedSec,
            hitFlash = false,
            specialGlow = (sin(totalElapsedSec * 2.2f) > 0.55f),
            smokeCloaked = false
        )
        val turntableYaw = sin(totalElapsedSec * 1.1f) * 32f
        rigRenderer.renderFighter(snap, this, previewYawOverrideDeg = turntableYaw)
    }

    private fun renderArenaBattleFrame(engine: CombatEngine) {
        val (playerSnap, oppSnap) = engine.copyRenderState(
            outPillars = renderPillars,
            outZones = renderZones,
            outProjectiles = renderProjectiles,
            outParticles = renderParticles
        )

        val flashAmt = engine.arenaFlashIntensity * 0.25f
        val flashR = ((engine.arenaFlashColorHex shr 16) and 0xFF) / 255f
        val flashG = ((engine.arenaFlashColorHex shr 8) and 0xFF) / 255f
        val flashB = (engine.arenaFlashColorHex and 0xFF) / 255f

        val skyR = 0.025f + flashR * flashAmt
        val skyG = 0.038f + flashG * flashAmt
        val skyB = 0.078f + flashB * flashAmt
        GLES20.glClearColor(skyR, skyG, skyB, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Dynamic fighting camera: frames both fighters, zooms in on Ultimate casts, applies screen shake
        val midX = (playerSnap.x + oppSnap.x) * 0.5f
        val separation = abs(playerSnap.x - oppSnap.x)
        val baseDistZ = (6.4f + separation * 0.58f).coerceIn(6.4f, 11.4f)
        val ultZoomZ = baseDistZ * (1f - engine.cameraZoomUltimate * 0.26f)
        val shake = engine.cameraShakeIntensity
        val shakeX = if (shake > 0.01f) sin(totalElapsedSec * 57f) * shake * 0.24f else 0f
        val shakeY = if (shake > 0.01f) cos(totalElapsedSec * 63f) * shake * 0.18f else 0f

        camEyeX = (midX * 0.78f) + shakeX
        camEyeY = (2.35f - engine.cameraZoomUltimate * 0.45f) + shakeY
        camEyeZ = ultZoomZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            camEyeX, camEyeY, camEyeZ,
            midX + shakeX * 0.5f, 1.25f + shakeY * 0.5f, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(shaderProgram)
        GLES20.glUniform3f(uLightDirLoc, 0.28f, 0.88f, 0.42f)
        GLES20.glUniform3f(uFogColorLoc, skyR, skyG, skyB)
        GLES20.glUniform3f(uCameraPosLoc, camEyeX, camEyeY, camEyeZ)

        // 1. Night Mountain Silhouette Backdrop & Futuristic Beast Battle Arena Platform
        renderBeastBattleArenaEnvironment()

        // 2. Destructible Cyber-Stone Pillars (intact, cracked, or shattered rubble)
        renderArenaPillars()

        // 3. Environmental Tactical Floor Zones (Shattered Floor Cracks, Scorched Flames, Smoke Clouds)
        renderTacticalZones()

        // 4. Both Articulated 3D Animal Warriors
        rigRenderer.renderFighter(playerSnap, this)
        rigRenderer.renderFighter(oppSnap, this)

        // 5. Travelling 3D Supernatural Projectiles
        renderProjectiles3D()

        // 6. 3D Sparks, Dust, Debris, and Fire Particles
        renderParticles3D()
    }

    private fun renderBeastBattleArenaEnvironment() {
        // Distant jagged night mountains (-Z background)
        val mountainConfigs = arrayOf(
            floatArrayOf(-15f, 4.5f, -24f, 9.5f, 11.0f, 7.0f),
            floatArrayOf(-6.5f, 5.8f, -26f, 11.0f, 13.5f, 8.0f),
            floatArrayOf(3.5f, 6.2f, -27f, 12.0f, 14.5f, 8.5f),
            floatArrayOf(13.5f, 4.8f, -24f, 10.0f, 11.5f, 7.5f)
        )
        for (m in mountainConfigs) {
            Matrix.setIdentityM(tempMatrix, 0)
            Matrix.translateM(tempMatrix, 0, m[0], m[1] - 1.2f, m[2])
            Matrix.scaleM(tempMatrix, 0, m[3], m[4], m[5])
            drawPart(coneMesh, tempMatrix, 0xFF0D1322.toInt(), emissive = 0.02f)
        }

        // Main Octagonal Futuristic Platform Deck
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0f, -0.55f, 0f)
        Matrix.scaleM(tempMatrix, 0, 17.6f, 1.10f, 9.2f)
        drawPart(prismMesh, tempMatrix, 0xFF141B29.toInt(), emissive = 0.04f)

        // Inner Combat Tatami / Cyber-Steel Ring
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0f, -0.015f, 0f)
        Matrix.scaleM(tempMatrix, 0, 15.6f, 0.04f, 6.8f)
        drawPart(prismMesh, tempMatrix, 0xFF1E273B.toInt(), emissive = 0.06f)

        // Left Neon Cyan Rim Strip & Right Neon Crimson Rim Strip
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, -4.1f, 0.01f, 2.55f)
        Matrix.scaleM(tempMatrix, 0, 7.2f, 0.04f, 0.14f)
        drawPart(boxMesh, tempMatrix, 0xFF00E5FF.toInt(), emissive = 0.92f)

        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 4.1f, 0.01f, 2.55f)
        Matrix.scaleM(tempMatrix, 0, 7.2f, 0.04f, 0.14f)
        drawPart(boxMesh, tempMatrix, 0xFFFF1744.toInt(), emissive = 0.92f)

        // Back Arena Neon Energy Pylons (decorative background stadium towers)
        for (side in intArrayOf(-1, 1)) {
            val neon = if (side < 0) 0xFF00E5FF.toInt() else 0xFFFF1744.toInt()
            Matrix.setIdentityM(tempMatrix, 0)
            Matrix.translateM(tempMatrix, 0, side * 8.2f, 2.4f, -3.8f)
            Matrix.scaleM(tempMatrix, 0, 0.65f, 5.2f, 0.65f)
            drawPart(boxMesh, tempMatrix, 0xFF121826.toInt(), emissive = 0.05f)

            Matrix.setIdentityM(tempMatrix, 0)
            Matrix.translateM(tempMatrix, 0, side * 8.2f, 2.4f, -3.45f)
            Matrix.scaleM(tempMatrix, 0, 0.16f, 4.8f, 0.10f)
            drawPart(boxMesh, tempMatrix, neon, emissive = 0.95f)
        }
    }

    private fun renderArenaPillars() {
        for (p in renderPillars) {
            val stoneColor = when {
                p.hitFlash > 0f -> 0xFFFFFFFF.toInt()
                p.integrity <= 50f -> 0xFF37474F.toInt()
                else -> 0xFF263238.toInt()
            }

            // Pillar base pedestal (always remains)
            Matrix.setIdentityM(tempMatrix, 0)
            Matrix.translateM(tempMatrix, 0, p.x, 0.18f, p.z)
            Matrix.scaleM(tempMatrix, 0, 0.95f, 0.36f, 0.95f)
            drawPart(prismMesh, tempMatrix, 0xFF1B2333.toInt(), emissive = 0.08f)

            if (!p.isBroken) {
                // Intact or Cracked Destructible Cyber-Pillar
                val tiltDeg = if (p.integrity <= 50f) 5.5f else 0f
                Matrix.setIdentityM(tempMatrix, 0)
                Matrix.translateM(tempMatrix, 0, p.x, 1.48f, p.z)
                Matrix.rotateM(tempMatrix, 0, tiltDeg, 0f, 0f, 1f)
                Matrix.scaleM(tempMatrix, 0, 0.66f, 2.35f, 0.66f)
                drawPart(prismMesh, tempMatrix, stoneColor, emissive = if (p.hitFlash > 0f) 0.7f else 0.06f)

                // Glowing Neon Core Strip along the pillar
                Matrix.setIdentityM(tempMatrix, 0)
                Matrix.translateM(tempMatrix, 0, p.x, 1.48f, p.z + 0.32f)
                Matrix.rotateM(tempMatrix, 0, tiltDeg, 0f, 0f, 1f)
                Matrix.scaleM(tempMatrix, 0, 0.14f, 2.15f, 0.08f)
                drawPart(boxMesh, tempMatrix, p.neonHex, emissive = 0.92f)
            } else {
                // Shattered Pillar Stump + Fallen Glowing Rubble Blocks!
                val fall = p.collapseAnim
                Matrix.setIdentityM(tempMatrix, 0)
                Matrix.translateM(tempMatrix, 0, p.x, 0.48f, p.z)
                Matrix.rotateM(tempMatrix, 0, 18f, 0f, 0f, 1f)
                Matrix.scaleM(tempMatrix, 0, 0.62f, 0.52f, 0.62f)
                drawPart(coneMesh, tempMatrix, 0xFF37474F.toInt(), emissive = 0.1f)

                // Fallen top chunk
                Matrix.setIdentityM(tempMatrix, 0)
                Matrix.translateM(tempMatrix, 0, p.x + 0.55f * fall, 1.2f * (1f - fall) + 0.22f, p.z + 0.25f)
                Matrix.rotateM(tempMatrix, 0, 82f * fall, 0f, 0f, 1f)
                Matrix.scaleM(tempMatrix, 0, 0.46f, 0.95f, 0.46f)
                drawPart(prismMesh, tempMatrix, 0xFF2C3846.toInt(), emissive = 0.05f)
            }
        }
    }

    private fun renderTacticalZones() {
        for (z in renderZones) {
            val fade = (z.remainingTime / z.maxTime.coerceAtLeast(0.1f)).coerceIn(0.15f, 1f)
            when (z.type) {
                ArenaHazardType.SHATTERED_FLOOR -> {
                    // Cracked jagged floor ring + glowing magma/seismic fissure lines
                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, z.x, 0.02f, z.z)
                    Matrix.scaleM(tempMatrix, 0, z.radius * 1.9f, 0.03f, z.radius * 1.2f)
                    drawPart(prismMesh, tempMatrix, 0xFF211814.toInt(), emissive = 0.18f, alpha = 0.88f * fade)

                    for (k in 0..2) {
                        Matrix.setIdentityM(tempMatrix, 0)
                        Matrix.translateM(tempMatrix, 0, z.x + (k - 1) * 0.45f, 0.035f, z.z)
                        Matrix.rotateM(tempMatrix, 0, k * 52f + 15f, 0f, 1f, 0f)
                        Matrix.scaleM(tempMatrix, 0, z.radius * 1.4f, 0.03f, 0.09f)
                        drawPart(boxMesh, tempMatrix, 0xFFFF8F00.toInt(), emissive = 0.85f, alpha = fade)
                    }
                }
                ArenaHazardType.SCORCHED_FLAME_ZONE -> {
                    val flicker = 0.85f + 0.2f * sin(totalElapsedSec * 18f + z.id)
                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, z.x, 0.03f, z.z)
                    Matrix.scaleM(tempMatrix, 0, z.radius * 1.8f * flicker, 0.05f, z.radius * 1.2f)
                    drawPart(prismMesh, tempMatrix, 0xFFFF3D00.toInt(), emissive = 0.95f, alpha = 0.78f * fade)
                }
                ArenaHazardType.DUST_SMOKE_CLOUD -> {
                    // Volumetric-looking semi-transparent billowing smoke spheres
                    for (puff in 0..3) {
                        val angle = totalElapsedSec * 1.4f + puff * 1.57f
                        val px = z.x + cos(angle) * (z.radius * 0.42f)
                        val py = 0.55f + (puff % 2) * 0.38f + 0.12f * sin(totalElapsedSec * 3f + puff)
                        val pz = z.z + sin(angle) * 0.35f
                        Matrix.setIdentityM(tempMatrix, 0)
                        Matrix.translateM(tempMatrix, 0, px, py, pz)
                        val s = 0.78f + (puff % 2) * 0.25f
                        Matrix.scaleM(tempMatrix, 0, s, s * 0.75f, s)
                        drawPart(sphereMesh, tempMatrix, 0xFF78909C.toInt(), emissive = 0.15f, alpha = 0.34f * fade)
                    }
                }
                ArenaHazardType.DESTRUCTIBLE_PILLAR -> Unit
            }
        }
    }

    private fun renderProjectiles3D() {
        for (pr in renderProjectiles) {
            when (pr.sourceFighter) {
                FighterId.IRON_BUFFALO -> {
                    // Compressed dark-orange shockwave ring
                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, pr.x, pr.y, pr.z)
                    Matrix.rotateM(tempMatrix, 0, 90f, 0f, 0f, 1f)
                    Matrix.scaleM(tempMatrix, 0, 0.85f, 0.22f, 0.85f)
                    drawPart(prismMesh, tempMatrix, pr.colorHex, emissive = 0.95f, alpha = 0.92f)
                }
                FighterId.TITAN_RHINO -> {
                    // Tumbling compressed rock boulder blast
                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, pr.x, pr.y, pr.z)
                    Matrix.rotateM(tempMatrix, 0, totalElapsedSec * 380f, 1f, 1f, 0f)
                    Matrix.scaleM(tempMatrix, 0, 0.58f, 0.58f, 0.58f)
                    drawPart(prismMesh, tempMatrix, pr.secondaryHex, emissive = 0.45f)
                }
                FighterId.SHADOW_TIGER -> {
                    // Twin glowing blue tiger-claw energy crescents
                    for (slash in -1..1) {
                        Matrix.setIdentityM(tempMatrix, 0)
                        Matrix.translateM(tempMatrix, 0, pr.x, pr.y + slash * 0.16f, pr.z)
                        Matrix.rotateM(tempMatrix, 0, if (pr.vx >= 0f) -90f else 90f, 0f, 0f, 1f)
                        Matrix.scaleM(tempMatrix, 0, 0.14f, 0.68f, 0.10f)
                        drawPart(coneMesh, tempMatrix, pr.colorHex, emissive = 1.0f)
                    }
                }
                FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> {
                    // Blazing draconic fireball core + outer plasma halo
                    val s = pr.radius * 2.1f
                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, pr.x, pr.y, pr.z)
                    Matrix.scaleM(tempMatrix, 0, s, s, s)
                    drawPart(sphereMesh, tempMatrix, pr.colorHex, emissive = 1.0f)

                    Matrix.setIdentityM(tempMatrix, 0)
                    Matrix.translateM(tempMatrix, 0, pr.x, pr.y, pr.z)
                    Matrix.scaleM(tempMatrix, 0, s * 1.45f, s * 1.45f, s * 1.45f)
                    drawPart(sphereMesh, tempMatrix, pr.secondaryHex, emissive = 0.85f, alpha = 0.48f)
                }
            }
        }
    }

    private fun renderParticles3D() {
        for (pt in renderParticles) {
            val alpha = (pt.life / pt.maxLife.coerceAtLeast(0.05f)).coerceIn(0.1f, 0.95f)
            Matrix.setIdentityM(tempMatrix, 0)
            Matrix.translateM(tempMatrix, 0, pt.x, pt.y, pt.z)
            Matrix.scaleM(tempMatrix, 0, pt.size, pt.size, pt.size)
            drawPart(boxMesh, tempMatrix, pt.colorHex, emissive = 0.75f, alpha = alpha)
        }
    }

    override fun drawPart(
        mesh: MeshBuffer3D,
        modelMatrix: FloatArray,
        colorHex: Int,
        emissive: Float,
        alpha: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val r = ((colorHex shr 16) and 0xFF) / 255f
        val g = ((colorHex shr 8) and 0xFF) / 255f
        val b = (colorHex and 0xFF) / 255f

        GLES20.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorLoc, r, g, b, alpha)
        GLES20.glUniform1f(uEmissiveLoc, emissive)

        val stride = 6 * 4
        mesh.vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, mesh.vertexBuffer)

        mesh.vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, mesh.vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun buildShaderProgram(vsSource: String, fsSource: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform float uEmissive;
            uniform vec3 uLightDir;
            uniform vec3 uFogColor;
            uniform vec3 uCameraPos;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec3 n = normalize(vWorldNormal);
                float keyDiff = max(dot(n, normalize(uLightDir)), 0.0);
                // Dual neon arena rim lights: Cyan from left (-X), Crimson from right (+X)
                float cyanRim = max(dot(n, normalize(vec3(-1.0, 0.35, 0.35))), 0.0) * 0.26;
                float redRim = max(dot(n, normalize(vec3(1.0, 0.35, 0.35))), 0.0) * 0.24;
                vec3 lit = uColor.rgb * (0.34 + 0.68 * keyDiff)
                         + vec3(0.0, 0.85, 1.0) * cyanRim * (1.0 - uEmissive)
                         + vec3(1.0, 0.12, 0.28) * redRim * (1.0 - uEmissive);
                vec3 finalRgb = mix(lit, uColor.rgb * 1.28, clamp(uEmissive, 0.0, 1.0));
                float dist = length(vWorldPos - uCameraPos);
                float fogFactor = clamp((dist - 14.0) / 26.0, 0.0, 0.72);
                finalRgb = mix(finalRgb, uFogColor, fogFactor * (1.0 - uEmissive));
                gl_FragColor = vec4(finalRgb, uColor.a);
            }
        """
    }
}
