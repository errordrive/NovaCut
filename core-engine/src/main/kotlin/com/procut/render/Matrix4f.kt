package com.procut.render

import kotlin.math.cos
import kotlin.math.sin

/**
 * 4x4 Column-Major Matrix for OpenGL ES 3.0 Model-View-Projection (MVP) transformations.
 */
class Matrix4f(val values: FloatArray = FloatArray(16)) {

    init {
        if (values.all { it == 0f }) {
            loadIdentity()
        }
    }

    fun loadIdentity(): Matrix4f {
        for (i in 0 until 16) {
            values[i] = if (i % 5 == 0) 1.0f else 0.0f
        }
        return this
    }

    fun clone(): Matrix4f {
        val result = Matrix4f()
        System.arraycopy(values, 0, result.values, 0, 16)
        return result
    }

    operator fun get(col: Int, row: Int): Float = values[col * 4 + row]
    operator fun set(col: Int, row: Int, value: Float) {
        values[col * 4 + row] = value
    }

    /**
     * Multiplies this matrix with [other] (this * other) and returns a new Matrix4f.
     */
    fun multiply(other: Matrix4f): Matrix4f {
        val result = Matrix4f()
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0.0f
                for (k in 0 until 4) {
                    sum += this[k, row] * other[col, k]
                }
                result[col, row] = sum
            }
        }
        return result
    }

    /**
     * In-place multiplication: this = this * other
     */
    fun multiplyInPlace(other: Matrix4f): Matrix4f {
        val temp = multiply(other)
        System.arraycopy(temp.values, 0, this.values, 0, 16)
        return this
    }

    fun translate(x: Float, y: Float, z: Float = 0f): Matrix4f {
        val translationMatrix = Matrix4f()
        translationMatrix[3, 0] = x
        translationMatrix[3, 1] = y
        translationMatrix[3, 2] = z
        return multiplyInPlace(translationMatrix)
    }

    fun scale(sx: Float, sy: Float, sz: Float = 1f): Matrix4f {
        val scaleMatrix = Matrix4f()
        scaleMatrix[0, 0] = sx
        scaleMatrix[1, 1] = sy
        scaleMatrix[2, 2] = sz
        return multiplyInPlace(scaleMatrix)
    }

    fun rotateZ(degrees: Float): Matrix4f {
        val radians = Math.toRadians(degrees.toDouble()).toFloat()
        val c = cos(radians)
        val s = sin(radians)

        val rotMatrix = Matrix4f()
        rotMatrix[0, 0] = c
        rotMatrix[0, 1] = s
        rotMatrix[1, 0] = -s
        rotMatrix[1, 1] = c
        return multiplyInPlace(rotMatrix)
    }

    /**
     * Sets orthographic projection matrix.
     */
    fun setOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float = -1f, far: Float = 1f): Matrix4f {
        val rml = right - left
        val tmb = top - bottom
        val fmn = far - near

        require(rml != 0f && tmb != 0f && fmn != 0f) { "Ortho bounds cannot be zero width/height/depth" }

        loadIdentity()
        this[0, 0] = 2.0f / rml
        this[1, 1] = 2.0f / tmb
        this[2, 2] = -2.0f / fmn
        this[3, 0] = -(right + left) / rml
        this[3, 1] = -(top + bottom) / tmb
        this[3, 2] = -(far + near) / fmn
        this[3, 3] = 1.0f
        return this
    }

    companion object {
        fun identity(): Matrix4f = Matrix4f().loadIdentity()

        fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float = -1f, far: Float = 1f): Matrix4f {
            return Matrix4f().setOrtho(left, right, bottom, top, near, far)
        }

        /**
         * Calculates Model-View-Projection matrix for a video layer on a canvas.
         */
        fun calculateLayerMVP(
            canvasWidth: Int,
            canvasHeight: Int,
            videoWidth: Int,
            videoHeight: Int,
            translationX: Float = 0f, // Normalized screen offset [-1, 1]
            translationY: Float = 0f,
            scaleX: Float = 1f,
            scaleY: Float = 1f,
            rotationDeg: Float = 0f
        ): Matrix4f {
            val canvasAspect = canvasWidth.toFloat() / canvasHeight.toFloat()
            val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

            // Calculate fit scale (letterbox/pillarbox)
            val fitScaleX: Float
            val fitScaleY: Float
            if (videoAspect > canvasAspect) {
                // Video is wider than canvas -> fit horizontally, letterbox vertically
                fitScaleX = 1.0f
                fitScaleY = canvasAspect / videoAspect
            } else {
                // Video is taller than canvas -> fit vertically, pillarbox horizontally
                fitScaleX = videoAspect / canvasAspect
                fitScaleY = 1.0f
            }

            val mvp = Matrix4f.identity()
            // 1. Translate (normalized -1 to 1)
            mvp.translate(translationX, translationY, 0f)
            // 2. Rotate around Z axis (center)
            mvp.rotateZ(rotationDeg)
            // 3. Scale by user scale * fit scale
            mvp.scale(fitScaleX * scaleX, fitScaleY * scaleY, 1.0f)

            return mvp
        }
    }
}
