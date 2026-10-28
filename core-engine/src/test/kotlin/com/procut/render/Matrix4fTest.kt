package com.procut.render

import org.junit.Assert.*
import org.junit.Test

class Matrix4fTest {

    @Test
    fun testIdentityMatrix() {
        val mat = Matrix4f.identity()
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                val expected = if (col == row) 1.0f else 0.0f
                assertEquals(expected, mat[col, row], 0.0001f)
            }
        }
    }

    @Test
    fun testTranslationMultiplication() {
        val mat = Matrix4f.identity()
        mat.translate(0.5f, -0.25f, 0.1f)

        assertEquals(0.5f, mat[3, 0], 0.0001f)
        assertEquals(-0.25f, mat[3, 1], 0.0001f)
        assertEquals(0.1f, mat[3, 2], 0.0001f)
        assertEquals(1.0f, mat[3, 3], 0.0001f)
    }

    @Test
    fun testScaleMultiplication() {
        val mat = Matrix4f.identity()
        mat.scale(2.0f, 3.0f, 1.0f)

        assertEquals(2.0f, mat[0, 0], 0.0001f)
        assertEquals(3.0f, mat[1, 1], 0.0001f)
        assertEquals(1.0f, mat[2, 2], 0.0001f)
    }

    @Test
    fun testRotateZMultiplication() {
        val mat = Matrix4f.identity()
        mat.rotateZ(90f)

        // cos(90) = 0, sin(90) = 1
        assertEquals(0.0f, mat[0, 0], 0.0001f)
        assertEquals(1.0f, mat[0, 1], 0.0001f)
        assertEquals(-1.0f, mat[1, 0], 0.0001f)
        assertEquals(0.0f, mat[1, 1], 0.0001f)
    }

    @Test
    fun testLayerMVPAspectCalculation() {
        // 9:16 vertical canvas (1080x1920)
        // 16:9 horizontal video (1920x1080)
        // Video is wider than canvas -> letterbox vertically
        val mvp = Matrix4f.calculateLayerMVP(
            canvasWidth = 1080,
            canvasHeight = 1920,
            videoWidth = 1920,
            videoHeight = 1080,
            translationX = 0f,
            translationY = 0f,
            scaleX = 1f,
            scaleY = 1f,
            rotationDeg = 0f
        )

        val canvasAspect = 1080f / 1920f
        val videoAspect = 1920f / 1080f
        val expectedFitScaleY = canvasAspect / videoAspect

        assertEquals(1.0f, mvp[0, 0], 0.001f) // fitScaleX
        assertEquals(expectedFitScaleY, mvp[1, 1], 0.001f) // fitScaleY
    }
}
