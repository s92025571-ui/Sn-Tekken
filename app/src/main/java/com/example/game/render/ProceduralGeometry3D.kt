package com.example.game.render

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MeshBuffer3D(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int
)

object ProceduralGeometry3D {

    private const val FLOATS_PER_VERTEX = 6 // x, y, z, nx, ny, nz

    /**
     * Unit centered cube [-0.5..0.5] in X, Y, Z with per-face normals (36 vertices).
     */
    fun createUnitBox(): MeshBuffer3D {
        val verts = ArrayList<Float>(36 * FLOATS_PER_VERTEX)

        fun quad(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            x4: Float, y4: Float, z4: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            addVertex(verts, x1, y1, z1, nx, ny, nz)
            addVertex(verts, x2, y2, z2, nx, ny, nz)
            addVertex(verts, x3, y3, z3, nx, ny, nz)
            addVertex(verts, x1, y1, z1, nx, ny, nz)
            addVertex(verts, x3, y3, z3, nx, ny, nz)
            addVertex(verts, x4, y4, z4, nx, ny, nz)
        }

        // Front (+Z)
        quad(-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f)
        // Back (-Z)
        quad(0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f)
        // Right (+X)
        quad(0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f)
        // Left (-X)
        quad(-0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f)
        // Top (+Y)
        quad(-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f)
        // Bottom (-Y)
        quad(-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f)

        return pack(verts)
    }

    /**
     * Cone / horn / spike pointing along +Y from y = -0.5 (base radius 0.5) to y = +0.5 (tip).
     */
    fun createCone(segments: Int = 10): MeshBuffer3D {
        val verts = ArrayList<Float>(segments * 6 * FLOATS_PER_VERTEX)
        val step = (2.0 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0)
            val s0 = sin(a0)
            val c1 = cos(a1)
            val s1 = sin(a1)

            val x0 = c0 * 0.5f
            val z0 = s0 * 0.5f
            val x1 = c1 * 0.5f
            val z1 = s1 * 0.5f

            // Side triangle
            val nx0 = c0 * 0.89f
            val nz0 = s0 * 0.89f
            val nx1 = c1 * 0.89f
            val nz1 = s1 * 0.89f
            val ny = 0.45f
            addVertex(verts, x0, -0.5f, z0, nx0, ny, nz0)
            addVertex(verts, x1, -0.5f, z1, nx1, ny, nz1)
            addVertex(verts, 0f, 0.5f, 0f, (nx0 + nx1) * 0.5f, 0.7f, (nz0 + nz1) * 0.5f)

            // Base cap
            addVertex(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVertex(verts, x1, -0.5f, z1, 0f, -1f, 0f)
            addVertex(verts, x0, -0.5f, z0, 0f, -1f, 0f)
        }
        return pack(verts)
    }

    /**
     * Octagonal cylinder / arena platform / muscular limb prism from y = -0.5 to +0.5, radius 0.5.
     */
    fun createPrism(segments: Int = 8): MeshBuffer3D {
        val verts = ArrayList<Float>(segments * 12 * FLOATS_PER_VERTEX)
        val step = (2.0 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0)
            val s0 = sin(a0)
            val c1 = cos(a1)
            val s1 = sin(a1)

            val x0 = c0 * 0.5f
            val z0 = s0 * 0.5f
            val x1 = c1 * 0.5f
            val z1 = s1 * 0.5f

            // Side quad
            addVertex(verts, x0, -0.5f, z0, c0, 0f, s0)
            addVertex(verts, x1, -0.5f, z1, c1, 0f, s1)
            addVertex(verts, x1, 0.5f, z1, c1, 0f, s1)

            addVertex(verts, x0, -0.5f, z0, c0, 0f, s0)
            addVertex(verts, x1, 0.5f, z1, c1, 0f, s1)
            addVertex(verts, x0, 0.5f, z0, c0, 0f, s0)

            // Top cap
            addVertex(verts, 0f, 0.5f, 0f, 0f, 1f, 0f)
            addVertex(verts, x1, 0.5f, z1, 0f, 1f, 0f)
            addVertex(verts, x0, 0.5f, z0, 0f, 1f, 0f)

            // Bottom cap
            addVertex(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVertex(verts, x0, -0.5f, z0, 0f, -1f, 0f)
            addVertex(verts, x1, -0.5f, z1, 0f, -1f, 0f)
        }
        return pack(verts)
    }

    /**
     * Low-poly UV sphere radius 0.5 for energy orbs, fireballs, joints, eyes, and smoke puffs.
     */
    fun createSphere(stacks: Int = 8, slices: Int = 12): MeshBuffer3D {
        val verts = ArrayList<Float>(stacks * slices * 6 * FLOATS_PER_VERTEX)
        for (i in 0 until stacks) {
            val phi0 = PI * (-0.5 + i.toDouble() / stacks)
            val phi1 = PI * (-0.5 + (i + 1).toDouble() / stacks)
            val y0 = sin(phi0).toFloat()
            val r0 = cos(phi0).toFloat()
            val y1 = sin(phi1).toFloat()
            val r1 = cos(phi1).toFloat()

            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val c0 = cos(th0).toFloat()
                val s0 = sin(th0).toFloat()
                val c1 = cos(th1).toFloat()
                val s1 = sin(th1).toFloat()

                val nx00 = r0 * c0; val ny00 = y0; val nz00 = r0 * s0
                val nx10 = r0 * c1; val ny10 = y0; val nz10 = r0 * s1
                val nx11 = r1 * c1; val ny11 = y1; val nz11 = r1 * s1
                val nx01 = r1 * c0; val ny01 = y1; val nz01 = r1 * s0

                addVertex(verts, nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00)
                addVertex(verts, nx10 * 0.5f, ny10 * 0.5f, nz10 * 0.5f, nx10, ny10, nz10)
                addVertex(verts, nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11)

                addVertex(verts, nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00)
                addVertex(verts, nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11)
                addVertex(verts, nx01 * 0.5f, ny01 * 0.5f, nz01 * 0.5f, nx01, ny01, nz01)
            }
        }
        return pack(verts)
    }

    private fun addVertex(
        out: MutableList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        out.add(x)
        out.add(y)
        out.add(z)
        out.add(nx / len)
        out.add(ny / len)
        out.add(nz / len)
    }

    private fun pack(list: List<Float>): MeshBuffer3D {
        val arr = FloatArray(list.size)
        for (i in list.indices) arr[i] = list[i]
        val buf = ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(arr)
                position(0)
            }
        return MeshBuffer3D(buf, arr.size / FLOATS_PER_VERTEX)
    }
}
