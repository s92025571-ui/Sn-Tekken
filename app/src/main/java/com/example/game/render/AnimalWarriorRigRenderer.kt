package com.example.game.render

import android.opengl.Matrix
import com.example.game.engine.FighterRenderSnapshot
import com.example.game.model.ActionState
import com.example.game.model.FighterId
import com.example.game.model.MoveType
import kotlin.math.sin

interface PartDrawCallback {
    fun drawPart(
        mesh: MeshBuffer3D,
        modelMatrix: FloatArray,
        colorHex: Int,
        emissive: Float = 0f,
        alpha: Float = 1f
    )
}

/**
 * Articulates and draws recognizable multi-mesh 3D anthropomorphic animal warriors
 * (2 legs, upright martial arts posture, muscular torso/arms/claws, distinct animal heads,
 * muzzles, jaws, horns, ears, stripes, armor folds, tails, and dragon wings).
 */
class AnimalWarriorRigRenderer(
    private val boxMesh: MeshBuffer3D,
    private val coneMesh: MeshBuffer3D,
    private val prismMesh: MeshBuffer3D,
    private val sphereMesh: MeshBuffer3D
) {
    private val rootMat = FloatArray(16)
    private val torsoMat = FloatArray(16)
    private val headMat = FloatArray(16)
    private val limbMat = FloatArray(16)
    private val childMat = FloatArray(16)
    private val detailMat = FloatArray(16)
    private val scratchMat = FloatArray(16)

    fun renderFighter(
        snap: FighterRenderSnapshot,
        drawer: PartDrawCallback,
        previewYawOverrideDeg: Float? = null
    ) {
        val id = snap.profile.id
        val t = snap.animTime

        // Scale & proportions per warrior archetype
        val overallScale = when (id) {
            FighterId.IRON_BUFFALO -> 1.08f
            FighterId.TITAN_RHINO -> 1.10f
            FighterId.SHADOW_TIGER -> 0.98f
            FighterId.INFERNO_DRAGON -> 1.05f
            FighterId.SHADOW_DRAGON_OVERLORD -> 1.36f // Massive intimidating boss
        }

        val torsoWidth = when (id) {
            FighterId.IRON_BUFFALO -> 0.72f
            FighterId.TITAN_RHINO -> 0.76f
            FighterId.SHADOW_TIGER -> 0.54f
            FighterId.INFERNO_DRAGON -> 0.60f
            FighterId.SHADOW_DRAGON_OVERLORD -> 0.74f
        }
        val torsoDepth = when (id) {
            FighterId.IRON_BUFFALO, FighterId.TITAN_RHINO -> 0.46f
            else -> 0.36f
        }

        // Compute skeletal pose angles from ActionState & MoveType
        val pose = computePoseAngles(snap)

        val alpha = if (snap.smokeCloaked) 0.58f else 1.0f
        val primaryColor = if (snap.hitFlash) 0xFFFFFFFF.toInt() else snap.profile.primaryHex
        val secondaryColor = if (snap.hitFlash) 0xFFFFCDD2.toInt() else snap.profile.secondaryHex
        val glowColor = snap.profile.glowHex
        val glowEmissive = if (snap.specialGlow) 0.92f else 0.35f

        // Facing yaw: +1 faces right (+X -> yaw +90 deg so +Z model forward points along +X)
        val yawDeg = previewYawOverrideDeg ?: if (snap.facingSign >= 0f) 76f else -76f

        Matrix.setIdentityM(rootMat, 0)
        Matrix.translateM(rootMat, 0, snap.x, snap.y + pose.hipOffsetY * overallScale, snap.z)
        Matrix.rotateM(rootMat, 0, yawDeg + pose.bodyTwistYaw, 0f, 1f, 0f)
        Matrix.scaleM(rootMat, 0, overallScale, overallScale, overallScale)

        // 1. Ground contact shadow disc
        Matrix.setIdentityM(scratchMat, 0)
        Matrix.translateM(scratchMat, 0, snap.x, 0.015f, snap.z)
        Matrix.scaleM(scratchMat, 0, 1.15f * overallScale, 0.02f, 0.85f * overallScale)
        drawer.drawPart(prismMesh, scratchMat, 0xFF04060A.toInt(), emissive = 0f, alpha = 0.65f)

        // 2. Pelvis / Martial Arts Belt
        buildLocalPart(rootMat, 0f, 0.88f, 0f, torsoWidth * 0.84f, 0.24f, torsoDepth * 0.86f, scratchMat)
        drawer.drawPart(boxMesh, scratchMat, secondaryColor, emissive = 0.05f, alpha = alpha)

        // Belt sash accent
        buildLocalPart(rootMat, 0f, 0.92f, 0.02f, torsoWidth * 0.88f, 0.10f, torsoDepth * 0.90f, scratchMat)
        drawer.drawPart(boxMesh, scratchMat, glowColor, emissive = glowEmissive * 0.6f, alpha = alpha)

        // 3. Two upright humanoid muscular legs (-X left, +X right in model space)
        renderLeg(
            parent = rootMat,
            sideSign = -1f,
            hipPitch = pose.leftLegPitch,
            kneePitch = pose.leftKneePitch,
            id = id,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            glowColor = glowColor,
            alpha = alpha,
            drawer = drawer
        )
        renderLeg(
            parent = rootMat,
            sideSign = 1f,
            hipPitch = pose.rightLegPitch,
            kneePitch = pose.rightKneePitch,
            id = id,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            glowColor = glowColor,
            alpha = alpha,
            drawer = drawer
        )

        // 4. Muscular Upper Torso & Chest
        Matrix.setIdentityM(torsoMat, 0)
        Matrix.translateM(torsoMat, 0, 0f, 0.98f, 0f)
        Matrix.rotateM(torsoMat, 0, pose.torsoPitch, 1f, 0f, 0f)
        Matrix.rotateM(torsoMat, 0, pose.torsoYaw, 0f, 1f, 0f)
        Matrix.multiplyMM(torsoMat, 0, rootMat, 0, torsoMat.clone(), 0)

        // Lower abdomen
        buildLocalPart(torsoMat, 0f, 0.22f, 0f, torsoWidth * 0.86f, 0.34f, torsoDepth * 0.88f, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

        // Broad muscular chest
        buildLocalPart(torsoMat, 0f, 0.54f, 0.02f, torsoWidth * 1.08f, 0.42f, torsoDepth * 1.04f, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

        // Species torso anatomy details (stripes, armor plates, pectoral scales, traps)
        renderTorsoAnatomyDetails(torsoMat, id, torsoWidth, torsoDepth, secondaryColor, glowColor, glowEmissive, alpha, drawer)

        // 5. Tail (Tiger striped tail, Dragon & Overlord long spiked tail, Buffalo/Rhino compact tail)
        renderTail(rootMat, id, t, primaryColor, secondaryColor, glowColor, glowEmissive, alpha, drawer)

        // 6. Wings (Inferno Dragon & Shadow Dragon Overlord folded/flaring dragon wings)
        if (id == FighterId.INFERNO_DRAGON || id == FighterId.SHADOW_DRAGON_OVERLORD) {
            renderDragonWings(torsoMat, id, t, snap.specialGlow, primaryColor, secondaryColor, glowColor, glowEmissive, alpha, drawer)
        }

        // 7. Two Muscular Arms, Forearms & Claws/Fists
        renderArm(
            torso = torsoMat,
            sideSign = -1f,
            torsoWidth = torsoWidth,
            shoulderPitch = pose.leftArmPitch,
            shoulderRoll = -16f - pose.armSpread,
            elbowPitch = pose.leftElbowPitch,
            id = id,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            glowColor = glowColor,
            glowEmissive = glowEmissive,
            alpha = alpha,
            drawer = drawer
        )
        renderArm(
            torso = torsoMat,
            sideSign = 1f,
            torsoWidth = torsoWidth,
            shoulderPitch = pose.rightArmPitch,
            shoulderRoll = 16f + pose.armSpread,
            elbowPitch = pose.rightElbowPitch,
            id = id,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            glowColor = glowColor,
            glowEmissive = glowEmissive,
            alpha = alpha,
            drawer = drawer
        )

        // 8. Thick Neck & Distinct Animal Head (Muzzle, Jaw, Eyes, Horns, Ears, Fangs/Scales)
        Matrix.setIdentityM(headMat, 0)
        Matrix.translateM(headMat, 0, 0f, 0.84f, 0.06f)
        Matrix.rotateM(headMat, 0, pose.headPitch, 1f, 0f, 0f)
        Matrix.multiplyMM(headMat, 0, torsoMat, 0, headMat.clone(), 0)

        renderDistinctAnimalHead(
            headRoot = headMat,
            id = id,
            jawOpenDeg = pose.jawOpenDeg,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            glowColor = glowColor,
            specialGlow = snap.specialGlow,
            alpha = alpha,
            drawer = drawer
        )

        // 9. Special / Ultimate supernatural aura ring around warrior
        if (snap.specialGlow) {
            val pulse = 1.0f + 0.18f * sin(t * 16f)
            Matrix.setIdentityM(scratchMat, 0)
            Matrix.translateM(scratchMat, 0, snap.x, snap.y + 0.08f, snap.z)
            Matrix.scaleM(scratchMat, 0, 1.55f * pulse * overallScale, 0.06f, 1.55f * pulse * overallScale)
            drawer.drawPart(prismMesh, scratchMat, glowColor, emissive = 0.95f, alpha = 0.72f)
        }
    }

    private fun renderLeg(
        parent: FloatArray,
        sideSign: Float,
        hipPitch: Float,
        kneePitch: Float,
        id: FighterId,
        primaryColor: Int,
        secondaryColor: Int,
        glowColor: Int,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        val hipX = sideSign * 0.21f
        val thighThick = when (id) {
            FighterId.IRON_BUFFALO, FighterId.TITAN_RHINO -> 0.26f
            FighterId.SHADOW_TIGER -> 0.20f
            else -> 0.23f
        }

        Matrix.setIdentityM(limbMat, 0)
        Matrix.translateM(limbMat, 0, hipX, 0.84f, 0f)
        Matrix.rotateM(limbMat, 0, hipPitch, 1f, 0f, 0f)
        Matrix.multiplyMM(limbMat, 0, parent, 0, limbMat.clone(), 0)

        // Muscular Thigh
        buildLocalPart(limbMat, 0f, -0.22f, 0f, thighThick, 0.46f, thighThick * 1.05f, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

        if (id == FighterId.SHADOW_TIGER) {
            // Black tiger stripes on thigh
            buildLocalPart(limbMat, sideSign * 0.02f, -0.20f, 0.02f, thighThick * 1.06f, 0.06f, thighThick * 0.9f, scratchMat)
            drawer.drawPart(boxMesh, scratchMat, 0xFF141414.toInt(), alpha = alpha)
        } else if (id == FighterId.TITAN_RHINO) {
            // Armored knee/thigh fold
            buildLocalPart(limbMat, 0f, -0.36f, 0.04f, thighThick * 1.12f, 0.11f, thighThick * 1.10f, scratchMat)
            drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
        }

        // Knee & Lower Leg
        Matrix.setIdentityM(childMat, 0)
        Matrix.translateM(childMat, 0, 0f, -0.44f, 0f)
        Matrix.rotateM(childMat, 0, kneePitch, 1f, 0f, 0f)
        Matrix.multiplyMM(childMat, 0, limbMat, 0, childMat.clone(), 0)

        buildLocalPart(childMat, 0f, -0.20f, 0f, thighThick * 0.85f, 0.42f, thighThick * 0.88f, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

        // Species Foot: Large Buffalo Hooves, Sturdy Rhino Feet, Clawed Tiger Paws, Digitigrade Dragon Talons
        when (id) {
            FighterId.IRON_BUFFALO -> {
                buildLocalPart(childMat, 0f, -0.40f, 0.06f, 0.25f, 0.14f, 0.34f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, 0xFF16181D.toInt(), alpha = alpha)
            }
            FighterId.TITAN_RHINO -> {
                buildLocalPart(childMat, 0f, -0.40f, 0.05f, 0.27f, 0.14f, 0.33f, scratchMat)
                drawer.drawPart(prismMesh, scratchMat, secondaryColor, alpha = alpha)
            }
            FighterId.SHADOW_TIGER -> {
                buildLocalPart(childMat, 0f, -0.40f, 0.07f, 0.20f, 0.11f, 0.30f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
            }
            FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> {
                buildLocalPart(childMat, 0f, -0.40f, 0.08f, 0.23f, 0.12f, 0.34f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)
                // Front dragon talons
                for (clawIdx in -1..1) {
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, clawIdx * 0.06f, -0.42f, 0.27f)
                    Matrix.rotateM(detailMat, 0, 80f, 1f, 0f, 0f)
                    Matrix.scaleM(detailMat, 0, 0.05f, 0.14f, 0.05f)
                    Matrix.multiplyMM(scratchMat, 0, childMat, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = 0.25f, alpha = alpha)
                }
            }
        }
    }

    private fun renderArm(
        torso: FloatArray,
        sideSign: Float,
        torsoWidth: Float,
        shoulderPitch: Float,
        shoulderRoll: Float,
        elbowPitch: Float,
        id: FighterId,
        primaryColor: Int,
        secondaryColor: Int,
        glowColor: Int,
        glowEmissive: Float,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        val shoulderX = sideSign * (torsoWidth * 0.62f)
        val armThick = when (id) {
            FighterId.IRON_BUFFALO, FighterId.TITAN_RHINO -> 0.24f
            FighterId.SHADOW_TIGER -> 0.17f
            else -> 0.20f
        }

        Matrix.setIdentityM(limbMat, 0)
        Matrix.translateM(limbMat, 0, shoulderX, 0.66f, 0.02f)
        Matrix.rotateM(limbMat, 0, shoulderPitch, 1f, 0f, 0f)
        Matrix.rotateM(limbMat, 0, shoulderRoll, 0f, 0f, 1f)
        Matrix.multiplyMM(limbMat, 0, torso, 0, limbMat.clone(), 0)

        // Deltoid Shoulder Cap
        buildLocalPart(limbMat, 0f, 0f, 0f, armThick * 1.32f, armThick * 1.25f, armThick * 1.32f, scratchMat)
        drawer.drawPart(sphereMesh, scratchMat, if (id == FighterId.TITAN_RHINO) secondaryColor else primaryColor, alpha = alpha)

        // Upper Arm Bicep
        buildLocalPart(limbMat, 0f, -0.20f, 0f, armThick, 0.38f, armThick, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

        if (id == FighterId.SHADOW_TIGER) {
            buildLocalPart(limbMat, 0f, -0.20f, 0f, armThick * 1.06f, 0.05f, armThick * 1.06f, scratchMat)
            drawer.drawPart(boxMesh, scratchMat, 0xFF141414.toInt(), alpha = alpha)
        }

        // Elbow & Forearm
        Matrix.setIdentityM(childMat, 0)
        Matrix.translateM(childMat, 0, 0f, -0.38f, 0f)
        Matrix.rotateM(childMat, 0, elbowPitch, 1f, 0f, 0f)
        Matrix.multiplyMM(childMat, 0, limbMat, 0, childMat.clone(), 0)

        buildLocalPart(childMat, 0f, -0.18f, 0f, armThick * 0.92f, 0.36f, armThick * 0.92f, scratchMat)
        drawer.drawPart(prismMesh, scratchMat, if (id == FighterId.TITAN_RHINO) secondaryColor else primaryColor, alpha = alpha)

        // Combat Gauntlet / Fist / Claws
        buildLocalPart(childMat, 0f, -0.38f, 0.02f, armThick * 1.08f, 0.16f, armThick * 1.12f, scratchMat)
        drawer.drawPart(boxMesh, scratchMat, secondaryColor, emissive = glowEmissive * 0.25f, alpha = alpha)

        // Claws for Tiger, Dragon, and Overlord
        if (id == FighterId.SHADOW_TIGER || id == FighterId.INFERNO_DRAGON || id == FighterId.SHADOW_DRAGON_OVERLORD) {
            for (c in -1..1) {
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, c * 0.05f, -0.48f, 0.04f)
                Matrix.rotateM(detailMat, 0, 180f, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.04f, 0.14f, 0.04f)
                Matrix.multiplyMM(scratchMat, 0, childMat, 0, detailMat, 0)
                drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = glowEmissive, alpha = alpha)
            }
        }
    }

    private fun renderTorsoAnatomyDetails(
        torso: FloatArray,
        id: FighterId,
        torsoWidth: Float,
        torsoDepth: Float,
        secondaryColor: Int,
        glowColor: Int,
        glowEmissive: Float,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        when (id) {
            FighterId.IRON_BUFFALO -> {
                // Massive buffalo shoulder-hump / trapezius muscle ridge
                buildLocalPart(torso, 0f, 0.74f, -0.04f, torsoWidth * 0.82f, 0.24f, torsoDepth * 0.92f, scratchMat)
                drawer.drawPart(sphereMesh, scratchMat, primaryOrDarker(0xFF1F2126.toInt()), alpha = alpha)
                // Glowing chest furnace vents during special
                buildLocalPart(torso, 0f, 0.54f, torsoDepth * 0.52f, torsoWidth * 0.48f, 0.12f, 0.05f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, glowColor, emissive = glowEmissive * 0.7f, alpha = alpha)
            }
            FighterId.TITAN_RHINO -> {
                // Layered thick gray natural armor skin folds on chest and shoulders
                buildLocalPart(torso, 0f, 0.62f, 0.03f, torsoWidth * 1.12f, 0.18f, torsoDepth * 1.10f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
                buildLocalPart(torso, 0f, 0.36f, 0.02f, torsoWidth * 0.96f, 0.14f, torsoDepth * 1.04f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
            }
            FighterId.SHADOW_TIGER -> {
                // White fur chest patch + bold black Bengal stripes
                buildLocalPart(torso, 0f, 0.52f, torsoDepth * 0.46f, torsoWidth * 0.58f, 0.34f, 0.08f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
                for (row in 0..2) {
                    val yOff = 0.30f + row * 0.16f
                    buildLocalPart(torso, 0f, yOff, -0.02f, torsoWidth * 1.06f, 0.045f, torsoDepth * 1.02f, scratchMat)
                    drawer.drawPart(boxMesh, scratchMat, 0xFF121212.toInt(), alpha = alpha)
                }
            }
            FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> {
                // Scaled draconic chest plates & dorsal spine ridges
                for (plate in 0..2) {
                    val yOff = 0.24f + plate * 0.17f
                    buildLocalPart(torso, 0f, yOff, torsoDepth * 0.48f, torsoWidth * 0.62f, 0.13f, 0.08f, scratchMat)
                    drawer.drawPart(boxMesh, scratchMat, secondaryColor, emissive = glowEmissive * 0.3f, alpha = alpha)

                    // Dorsal back spikes
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, 0f, yOff + 0.08f, -torsoDepth * 0.55f)
                    Matrix.rotateM(detailMat, 0, -75f, 1f, 0f, 0f)
                    Matrix.scaleM(detailMat, 0, 0.08f, 0.22f, 0.08f)
                    Matrix.multiplyMM(scratchMat, 0, torso, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = glowEmissive * 0.6f, alpha = alpha)
                }
            }
        }
    }

    /**
     * Builds unmistakable animal heads with muzzles, jaws, fangs/horns, ears, and glowing eyes.
     */
    private fun renderDistinctAnimalHead(
        headRoot: FloatArray,
        id: FighterId,
        jawOpenDeg: Float,
        primaryColor: Int,
        secondaryColor: Int,
        glowColor: Int,
        specialGlow: Boolean,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        val eyeEmissive = if (specialGlow) 1.0f else 0.78f

        when (id) {
            FighterId.IRON_BUFFALO -> {
                // Broad heavy Buffalo cranium
                buildLocalPart(headRoot, 0f, 0.12f, 0.04f, 0.42f, 0.34f, 0.40f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

                // Broad protruding Buffalo Muzzle & Snout
                buildLocalPart(headRoot, 0f, 0.03f, 0.28f, 0.36f, 0.22f, 0.30f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)

                // Lower articulated jaw
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, -0.08f, 0.20f)
                Matrix.rotateM(detailMat, 0, jawOpenDeg, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.30f, 0.10f, 0.28f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

                // Two thick outward-and-upward sweeping Water Buffalo Horns!
                for (side in intArrayOf(-1, 1)) {
                    // Horn base sweeping outward
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.26f, 0.24f, 0.02f)
                    Matrix.rotateM(detailMat, 0, -side * 68f, 0f, 0f, 1f)
                    Matrix.scaleM(detailMat, 0, 0.13f, 0.34f, 0.13f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(prismMesh, scratchMat, 0xFFCFD8DC.toInt(), alpha = alpha)

                    // Curved horn tip sweeping up & forward
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.44f, 0.38f, 0.10f)
                    Matrix.rotateM(detailMat, 0, -side * 18f, 0f, 0f, 1f)
                    Matrix.rotateM(detailMat, 0, 22f, 1f, 0f, 0f)
                    Matrix.scaleM(detailMat, 0, 0.11f, 0.34f, 0.11f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, if (specialGlow) glowColor else 0xFFECEFF1.toInt(), emissive = if (specialGlow) 0.85f else 0.1f, alpha = alpha)

                    // Buffalo ears below horns
                    buildLocalPart(headRoot, side * 0.24f, 0.14f, -0.02f, 0.14f, 0.08f, 0.08f, scratchMat)
                    drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

                    // Glowing orange eyes
                    buildLocalPart(headRoot, side * 0.14f, 0.16f, 0.24f, 0.07f, 0.05f, 0.06f, scratchMat)
                    drawer.drawPart(sphereMesh, scratchMat, 0xFFFF6D00.toInt(), emissive = eyeEmissive, alpha = alpha)
                }
            }

            FighterId.TITAN_RHINO -> {
                // Elongated heavy Rhinoceros skull
                buildLocalPart(headRoot, 0f, 0.13f, 0.05f, 0.38f, 0.32f, 0.44f, scratchMat)
                drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

                // Long thick armored Rhino snout
                buildLocalPart(headRoot, 0f, 0.08f, 0.32f, 0.30f, 0.24f, 0.34f, scratchMat)
                drawer.drawPart(prismMesh, scratchMat, secondaryColor, alpha = alpha)

                // Heavy lower jaw
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, -0.06f, 0.26f)
                Matrix.rotateM(detailMat, 0, jawOpenDeg, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.24f, 0.10f, 0.30f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

                // Signature Giant Front Nasal Rhino Horn + Secondary Brow Horn!
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, 0.30f, 0.44f)
                Matrix.rotateM(detailMat, 0, 28f, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.14f, 0.42f, 0.16f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(coneMesh, scratchMat, 0xFFE0E0E0.toInt(), emissive = if (specialGlow) 0.65f else 0.1f, alpha = alpha)

                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, 0.26f, 0.24f)
                Matrix.rotateM(detailMat, 0, 18f, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.10f, 0.22f, 0.11f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(coneMesh, scratchMat, 0xFFCFD8DC.toInt(), alpha = alpha)

                // Upright tubular Rhino ears & eyes
                for (side in intArrayOf(-1, 1)) {
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.16f, 0.32f, -0.08f)
                    Matrix.rotateM(detailMat, 0, -side * 16f, 0f, 0f, 1f)
                    Matrix.scaleM(detailMat, 0, 0.08f, 0.16f, 0.08f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, primaryColor, alpha = alpha)

                    buildLocalPart(headRoot, side * 0.15f, 0.15f, 0.22f, 0.06f, 0.05f, 0.06f, scratchMat)
                    drawer.drawPart(sphereMesh, scratchMat, 0xFF80D8FF.toInt(), emissive = eyeEmissive, alpha = alpha)
                }
            }

            FighterId.SHADOW_TIGER -> {
                // Feline Bengal Tiger cranium
                buildLocalPart(headRoot, 0f, 0.12f, 0.02f, 0.36f, 0.32f, 0.36f, scratchMat)
                drawer.drawPart(sphereMesh, scratchMat, primaryColor, alpha = alpha)

                // White cheek ruffs (signature Bengal tiger facial fur!)
                for (side in intArrayOf(-1, 1)) {
                    buildLocalPart(headRoot, side * 0.17f, 0.06f, 0.08f, 0.14f, 0.18f, 0.18f, scratchMat)
                    drawer.drawPart(sphereMesh, scratchMat, secondaryColor, alpha = alpha)

                    // Rounded upright tiger ears
                    buildLocalPart(headRoot, side * 0.14f, 0.30f, -0.02f, 0.10f, 0.12f, 0.06f, scratchMat)
                    drawer.drawPart(coneMesh, scratchMat, primaryColor, alpha = alpha)

                    // Expressive glowing golden/azure tiger eyes
                    buildLocalPart(headRoot, side * 0.10f, 0.15f, 0.19f, 0.065f, 0.05f, 0.05f, scratchMat)
                    drawer.drawPart(sphereMesh, scratchMat, if (specialGlow) glowColor else 0xFFFFD600.toInt(), emissive = eyeEmissive, alpha = alpha)

                    // Sharp upper canine fangs
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.07f, -0.02f, 0.29f)
                    Matrix.rotateM(detailMat, 0, 180f, 1f, 0f, 0f)
                    Matrix.scaleM(detailMat, 0, 0.035f, 0.10f, 0.035f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, 0xFFFFFFFF.toInt(), alpha = alpha)
                }

                // Tiger muzzle & dark nose
                buildLocalPart(headRoot, 0f, 0.06f, 0.24f, 0.22f, 0.15f, 0.20f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
                buildLocalPart(headRoot, 0f, 0.11f, 0.34f, 0.08f, 0.05f, 0.05f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, 0xFF1A1A1A.toInt(), alpha = alpha)

                // Black forehead stripe
                buildLocalPart(headRoot, 0f, 0.24f, 0.16f, 0.16f, 0.04f, 0.12f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, 0xFF121212.toInt(), alpha = alpha)

                // Articulated white tiger jaw
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, -0.05f, 0.20f)
                Matrix.rotateM(detailMat, 0, jawOpenDeg + 6f, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.18f, 0.08f, 0.20f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)
            }

            FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> {
                val isOverlord = id == FighterId.SHADOW_DRAGON_OVERLORD
                // Reptilian draconic skull
                buildLocalPart(headRoot, 0f, 0.14f, 0.02f, 0.36f, 0.30f, 0.40f, scratchMat)
                drawer.drawPart(prismMesh, scratchMat, primaryColor, alpha = alpha)

                // Long reptilian Dragon Muzzle & Snout
                buildLocalPart(headRoot, 0f, 0.10f, 0.32f, 0.24f, 0.18f, 0.38f, scratchMat)
                drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

                // Articulated toothed Dragon Jaw
                Matrix.setIdentityM(detailMat, 0)
                Matrix.translateM(detailMat, 0, 0f, -0.03f, 0.28f)
                Matrix.rotateM(detailMat, 0, jawOpenDeg + 10f, 1f, 0f, 0f)
                Matrix.scaleM(detailMat, 0, 0.21f, 0.09f, 0.36f)
                Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                drawer.drawPart(boxMesh, scratchMat, secondaryColor, alpha = alpha)

                // Swept-back Dragon Horns + Glowing Reptilian Eyes + Sharp Teeth
                for (side in intArrayOf(-1, 1)) {
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.15f, 0.32f, -0.14f)
                    Matrix.rotateM(detailMat, 0, -48f, 1f, 0f, 0f)
                    Matrix.rotateM(detailMat, 0, -side * 18f, 0f, 0f, 1f)
                    val hornLen = if (isOverlord) 0.52f else 0.38f
                    Matrix.scaleM(detailMat, 0, 0.10f, hornLen, 0.10f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, if (isOverlord) secondaryColor else 0xFFFFB300.toInt(), emissive = 0.25f, alpha = alpha)

                    // Extra crown horn pair for Shadow Dragon Overlord
                    if (isOverlord) {
                        Matrix.setIdentityM(detailMat, 0)
                        Matrix.translateM(detailMat, 0, side * 0.22f, 0.22f, -0.10f)
                        Matrix.rotateM(detailMat, 0, -35f, 1f, 0f, 0f)
                        Matrix.rotateM(detailMat, 0, -side * 38f, 0f, 0f, 1f)
                        Matrix.scaleM(detailMat, 0, 0.08f, 0.36f, 0.08f)
                        Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                        drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = 0.7f, alpha = alpha)
                    }

                    // Glowing draconic eyes
                    buildLocalPart(headRoot, side * 0.12f, 0.17f, 0.20f, 0.07f, 0.05f, 0.07f, scratchMat)
                    drawer.drawPart(sphereMesh, scratchMat, glowColor, emissive = 1.0f, alpha = alpha)

                    // Fangs
                    Matrix.setIdentityM(detailMat, 0)
                    Matrix.translateM(detailMat, 0, side * 0.08f, 0.0f, 0.42f)
                    Matrix.rotateM(detailMat, 0, 180f, 1f, 0f, 0f)
                    Matrix.scaleM(detailMat, 0, 0.035f, 0.09f, 0.035f)
                    Matrix.multiplyMM(scratchMat, 0, headRoot, 0, detailMat, 0)
                    drawer.drawPart(coneMesh, scratchMat, 0xFFFFFFFF.toInt(), alpha = alpha)
                }
            }
        }
    }

    private fun renderTail(
        root: FloatArray,
        id: FighterId,
        animTime: Float,
        primaryColor: Int,
        secondaryColor: Int,
        glowColor: Int,
        glowEmissive: Float,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        val segments = when (id) {
            FighterId.SHADOW_TIGER -> 4
            FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> 5
            else -> 2
        }
        val segLen = when (id) {
            FighterId.SHADOW_TIGER -> 0.24f
            FighterId.INFERNO_DRAGON, FighterId.SHADOW_DRAGON_OVERLORD -> 0.28f
            else -> 0.16f
        }
        val baseThick = if (id == FighterId.INFERNO_DRAGON || id == FighterId.SHADOW_DRAGON_OVERLORD) 0.18f else 0.10f

        System.arraycopy(root, 0, detailMat, 0, 16)
        Matrix.translateM(detailMat, 0, 0f, 0.86f, -0.18f)

        for (s in 0 until segments) {
            val sway = sin(animTime * 4.5f + s * 0.8f) * 14f
            Matrix.rotateM(detailMat, 0, -18f + s * 8f, 1f, 0f, 0f)
            Matrix.rotateM(detailMat, 0, sway, 0f, 1f, 0f)
            val taper = 1f - (s.toFloat() / (segments + 1)) * 0.55f
            buildLocalPart(detailMat, 0f, 0f, -segLen * 0.5f, baseThick * taper, baseThick * taper, segLen, scratchMat)
            val segColor = if (id == FighterId.SHADOW_TIGER && s % 2 == 1) 0xFF141414.toInt() else primaryColor
            drawer.drawPart(prismMesh, scratchMat, segColor, alpha = alpha)
            Matrix.translateM(detailMat, 0, 0f, 0f, -segLen)
        }

        if (id == FighterId.INFERNO_DRAGON || id == FighterId.SHADOW_DRAGON_OVERLORD) {
            // Spiked flame tip on dragon tail
            buildLocalPart(detailMat, 0f, 0f, -0.10f, 0.14f, 0.14f, 0.22f, scratchMat)
            drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = glowEmissive, alpha = alpha)
        }
    }

    private fun renderDragonWings(
        torso: FloatArray,
        id: FighterId,
        animTime: Float,
        specialGlow: Boolean,
        primaryColor: Int,
        secondaryColor: Int,
        glowColor: Int,
        glowEmissive: Float,
        alpha: Float,
        drawer: PartDrawCallback
    ) {
        val flap = sin(animTime * (if (specialGlow) 10f else 3.2f)) * (if (specialGlow) 22f else 9f)
        val wingSpan = if (id == FighterId.SHADOW_DRAGON_OVERLORD) 0.92f else 0.68f

        for (side in intArrayOf(-1, 1)) {
            Matrix.setIdentityM(limbMat, 0)
            Matrix.translateM(limbMat, 0, side * 0.22f, 0.62f, -0.18f)
            Matrix.rotateM(limbMat, 0, -28f - flap, 0f, 1f, 0f)
            Matrix.rotateM(limbMat, 0, side * (36f + flap * 0.5f), 0f, 0f, 1f)
            Matrix.multiplyMM(limbMat, 0, torso, 0, limbMat.clone(), 0)

            // Wing bone strut
            buildLocalPart(limbMat, side * wingSpan * 0.45f, 0.22f, -0.10f, wingSpan, 0.08f, 0.08f, scratchMat)
            drawer.drawPart(boxMesh, scratchMat, primaryColor, alpha = alpha)

            // Wing membrane blade
            buildLocalPart(limbMat, side * wingSpan * 0.48f, -0.04f, -0.14f, wingSpan * 0.95f, 0.46f, 0.03f, scratchMat)
            drawer.drawPart(boxMesh, scratchMat, secondaryColor, emissive = glowEmissive * 0.45f, alpha = alpha * 0.92f)

            // Glowing wing claw tip
            buildLocalPart(limbMat, side * wingSpan * 0.92f, 0.26f, -0.10f, 0.08f, 0.16f, 0.08f, scratchMat)
            drawer.drawPart(coneMesh, scratchMat, glowColor, emissive = glowEmissive, alpha = alpha)
        }
    }

    private fun buildLocalPart(
        parent: FloatArray,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        outMatrix: FloatArray
    ) {
        Matrix.setIdentityM(outMatrix, 0)
        Matrix.translateM(outMatrix, 0, tx, ty, tz)
        Matrix.scaleM(outMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(outMatrix, 0, parent, 0, outMatrix.clone(), 0)
    }

    private fun primaryOrDarker(hex: Int): Int = hex

    private data class RigPose(
        val hipOffsetY: Float = 0f,
        val bodyTwistYaw: Float = 0f,
        val torsoPitch: Float = 6f,
        val torsoYaw: Float = 0f,
        val headPitch: Float = 0f,
        val jawOpenDeg: Float = 4f,
        val leftArmPitch: Float = 36f,
        val rightArmPitch: Float = 42f,
        val leftElbowPitch: Float = 65f,
        val rightElbowPitch: Float = 72f,
        val armSpread: Float = 0f,
        val leftLegPitch: Float = 14f,
        val rightLegPitch: Float = -14f,
        val leftKneePitch: Float = -18f,
        val rightKneePitch: Float = -18f
    )

    private fun computePoseAngles(snap: FighterRenderSnapshot): RigPose {
        val t = snap.animTime
        val p = (snap.stateTimer / snap.moveDuration.coerceAtLeast(0.01f)).coerceIn(0f, 1f)

        return when (snap.state) {
            ActionState.INTRO -> {
                val roarWave = sin(t * 6f)
                RigPose(
                    hipOffsetY = -0.04f,
                    torsoPitch = -8f * roarWave,
                    jawOpenDeg = 22f,
                    leftArmPitch = 55f + 20f * roarWave,
                    rightArmPitch = 55f - 20f * roarWave,
                    armSpread = 18f
                )
            }
            ActionState.IDLE -> {
                val breath = sin(t * 4.2f)
                RigPose(
                    hipOffsetY = 0.03f * breath,
                    torsoPitch = 7f + 2f * breath,
                    torsoYaw = -12f,
                    leftArmPitch = 38f + 5f * breath,
                    rightArmPitch = 46f - 5f * breath,
                    leftElbowPitch = 68f,
                    rightElbowPitch = 78f,
                    leftLegPitch = 14f,
                    rightLegPitch = -14f
                )
            }
            ActionState.WALK_FORWARD, ActionState.WALK_BACKWARD -> {
                val stride = sin(t * 9.5f)
                RigPose(
                    hipOffsetY = 0.04f * kotlin.math.abs(stride),
                    torsoPitch = 10f,
                    leftLegPitch = 32f * stride,
                    rightLegPitch = -32f * stride,
                    leftKneePitch = -22f,
                    rightKneePitch = -22f,
                    leftArmPitch = 35f - 24f * stride,
                    rightArmPitch = 35f + 24f * stride
                )
            }
            ActionState.RUN -> {
                val stride = sin(t * 15f)
                RigPose(
                    hipOffsetY = 0.06f * kotlin.math.abs(stride),
                    torsoPitch = 24f,
                    leftLegPitch = 48f * stride,
                    rightLegPitch = -48f * stride,
                    leftKneePitch = -32f,
                    rightKneePitch = -32f,
                    leftArmPitch = 28f - 38f * stride,
                    rightArmPitch = 28f + 38f * stride
                )
            }
            ActionState.JUMP -> RigPose(
                torsoPitch = 12f,
                leftLegPitch = 38f,
                rightLegPitch = 22f,
                leftKneePitch = -55f,
                rightKneePitch = -45f,
                leftArmPitch = 65f,
                rightArmPitch = 65f,
                armSpread = 16f
            )
            ActionState.BLOCK -> RigPose(
                hipOffsetY = -0.08f,
                torsoPitch = 14f,
                leftArmPitch = 78f,
                rightArmPitch = 82f,
                leftElbowPitch = 95f,
                rightElbowPitch = 95f,
                leftLegPitch = 22f,
                rightLegPitch = -22f
            )
            ActionState.DODGE -> RigPose(
                hipOffsetY = -0.22f,
                torsoPitch = 34f,
                bodyTwistYaw = p * 360f,
                leftLegPitch = 30f,
                rightLegPitch = -30f
            )
            ActionState.HIT_STUN -> RigPose(
                torsoPitch = -24f,
                headPitch = -20f,
                jawOpenDeg = 18f,
                leftArmPitch = 15f,
                rightArmPitch = 15f,
                armSpread = 24f
            )
            ActionState.KNOCKDOWN -> RigPose(
                hipOffsetY = -0.58f,
                torsoPitch = -72f,
                leftLegPitch = 45f,
                rightLegPitch = 30f,
                leftArmPitch = -20f,
                rightArmPitch = -20f
            )
            ActionState.ATTACK_STARTUP,
            ActionState.ATTACK_ACTIVE,
            ActionState.ATTACK_RECOVERY -> {
                val thrust = sin(p * 3.14159f)
                when (snap.currentMove) {
                    MoveType.PUNCH -> RigPose(
                        torsoPitch = 16f * thrust,
                        torsoYaw = 28f * thrust,
                        rightArmPitch = 92f * thrust + 25f,
                        rightElbowPitch = 80f * (1f - thrust),
                        leftArmPitch = 35f,
                        jawOpenDeg = 14f * thrust
                    )
                    MoveType.KICK -> RigPose(
                        torsoPitch = -16f * thrust,
                        bodyTwistYaw = 24f * thrust,
                        rightLegPitch = 88f * thrust,
                        rightKneePitch = -10f * (1f - thrust),
                        leftLegPitch = -12f,
                        armSpread = 22f * thrust
                    )
                    MoveType.HEAVY -> RigPose(
                        hipOffsetY = -0.10f * thrust,
                        torsoPitch = 28f * thrust,
                        headPitch = 22f * thrust,
                        jawOpenDeg = 24f,
                        leftArmPitch = 105f * thrust,
                        rightArmPitch = 105f * thrust,
                        leftElbowPitch = 25f,
                        rightElbowPitch = 25f,
                        rightLegPitch = 45f * (1f - p)
                    )
                    MoveType.PROJECTILE, MoveType.BOSS_FLAME_BREATH -> RigPose(
                        torsoPitch = 18f,
                        jawOpenDeg = 32f * thrust,
                        leftArmPitch = 88f * thrust,
                        rightArmPitch = 88f * thrust,
                        leftElbowPitch = 15f,
                        rightElbowPitch = 15f,
                        armSpread = 12f
                    )
                    MoveType.BOSS_TAIL_SWEEP -> RigPose(
                        bodyTwistYaw = p * 360f,
                        torsoPitch = 22f,
                        armSpread = 26f
                    )
                    else -> RigPose(
                        torsoPitch = 32f * thrust,
                        headPitch = 18f * thrust,
                        jawOpenDeg = 28f,
                        rightArmPitch = 100f * thrust,
                        leftArmPitch = 82f * thrust,
                        rightElbowPitch = 18f,
                        armSpread = 20f
                    )
                }
            }
            ActionState.ULTIMATE_CAST -> {
                val rushCycle = sin(t * 24f)
                RigPose(
                    hipOffsetY = -0.06f,
                    torsoPitch = 26f,
                    torsoYaw = 24f * rushCycle,
                    jawOpenDeg = 34f,
                    leftArmPitch = 90f + 35f * rushCycle,
                    rightArmPitch = 90f - 35f * rushCycle,
                    leftElbowPitch = 20f,
                    rightElbowPitch = 20f,
                    armSpread = 24f,
                    leftLegPitch = 36f * rushCycle,
                    rightLegPitch = -36f * rushCycle
                )
            }
            ActionState.VICTORY -> {
                val pump = kotlin.math.abs(sin(t * 5f))
                RigPose(
                    torsoPitch = -12f,
                    jawOpenDeg = 26f * pump,
                    leftArmPitch = 145f + 15f * pump,
                    rightArmPitch = 145f + 15f * pump,
                    leftElbowPitch = 35f,
                    rightElbowPitch = 35f,
                    armSpread = 22f
                )
            }
            ActionState.DEFEAT -> RigPose(
                hipOffsetY = -0.68f,
                torsoPitch = -82f,
                headPitch = -25f,
                leftArmPitch = -35f,
                rightArmPitch = -35f,
                leftLegPitch = 25f,
                rightLegPitch = 15f
            )
        }
    }
}
