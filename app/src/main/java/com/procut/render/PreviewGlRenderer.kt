package com.procut.render

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.procut.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PreviewGlRenderer : GLSurfaceView.Renderer {

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var canvasSolidProgram: GlProgram? = null
    private var canvasGradientProgram: GlProgram? = null
    private var canvasBlurProgram: GlProgram? = null
    private var colorGradingProgram: GlProgram? = null
    private var chromaKeyProgram: GlProgram? = null
    private var geometricMaskProgram: GlProgram? = null
    private var transitionProgram: GlProgram? = null

    @Volatile
    var currentFrame: CompositionFrame? = null

    private var surfaceWidth: Int = 1080
    private var surfaceHeight: Int = 1920

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.07f, 0.07f, 0.07f, 1.0f) // #121212 Studio dark
        initGeometry()
        initPrograms()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val frame = currentFrame ?: return

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        for (pass in frame.passes) {
            when (pass) {
                is RenderPass.Background -> renderBackground(pass, frame.canvasWidth, frame.canvasHeight)
                is RenderPass.VideoLayer -> renderVideoLayer(pass, frame.canvasWidth, frame.canvasHeight)
                is RenderPass.TransitionPass -> renderTransition(pass)
            }
        }

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun initGeometry() {
        val quadCoords = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        val texCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)
    }

    private fun initPrograms() {
        canvasSolidProgram = GlProgram(ShaderSources.VERTEX_PASSTHROUGH, ShaderSources.FRAGMENT_CANVAS_SOLID)
        canvasGradientProgram = GlProgram(ShaderSources.VERTEX_PASSTHROUGH, ShaderSources.FRAGMENT_CANVAS_GRADIENT)
        canvasBlurProgram = GlProgram(ShaderSources.VERTEX_PASSTHROUGH, ShaderSources.FRAGMENT_CANVAS_GAUSSIAN_BLUR)
        colorGradingProgram = GlProgram(ShaderSources.VERTEX_TRANSFORM, ShaderSources.FRAGMENT_COLOR_GRADING)
        chromaKeyProgram = GlProgram(ShaderSources.VERTEX_TRANSFORM, ShaderSources.FRAGMENT_CHROMA_KEY)
        geometricMaskProgram = GlProgram(ShaderSources.VERTEX_PASSTHROUGH, ShaderSources.FRAGMENT_GEOMETRIC_MASK)
        transitionProgram = GlProgram(ShaderSources.VERTEX_PASSTHROUGH, ShaderSources.FRAGMENT_TRANSITION)
    }

    private fun renderBackground(pass: RenderPass.Background, canvasW: Int, canvasH: Int) {
        when (val bg = pass.canvasConfig.background) {
            is CanvasBackground.SolidColor -> {
                val program = canvasSolidProgram ?: return
                program.use()
                val parsedColor = parseColor(bg.colorHex)
                program.setUniform4f("u_BackgroundColor", parsedColor[0], parsedColor[1], parsedColor[2], parsedColor[3])
                drawQuad(program)
            }
            is CanvasBackground.Gradient -> {
                val program = canvasGradientProgram ?: return
                program.use()
                val start = parseColor(bg.startColorHex)
                val end = parseColor(bg.endColorHex)
                program.setUniform4f("u_StartColor", start[0], start[1], start[2], start[3])
                program.setUniform4f("u_EndColor", end[0], end[1], end[2], end[3])
                program.setUniform1f("u_AngleRad", Math.toRadians(bg.angleDegrees.toDouble()).toFloat())
                drawQuad(program)
            }
            is CanvasBackground.Blur -> {
                val mainTex = pass.mainVideoTextureId
                if (mainTex != null && mainTex != 0) {
                    val program = canvasBlurProgram ?: return
                    program.use()
                    bindTexture(GLES30.GL_TEXTURE0, mainTex)
                    program.setUniform1i("u_Texture", 0)
                    program.setUniform2f("u_Direction", 1.0f / canvasW, 0.0f)
                    program.setUniform1f("u_BlurRadius", bg.radius)
                    program.setUniform1f("u_Darken", bg.darken)
                    drawQuad(program)
                }
            }
        }
    }

    private fun renderVideoLayer(pass: RenderPass.VideoLayer, canvasW: Int, canvasH: Int) {
        val program = colorGradingProgram ?: return
        program.use()

        bindTexture(GLES30.GL_TEXTURE0, pass.textureId)
        program.setUniform1i("u_Texture", 0)
        program.setUniformMatrix4fv("u_MVPMatrix", pass.mvpMatrix)
        program.setUniformMatrix4fv("u_STMatrix", Matrix4f.identity())

        // 12 Color Adjustment uniforms
        val adj = pass.colorAdjustment
        program.setUniform1f("u_Brightness", adj.brightness)
        program.setUniform1f("u_Contrast", adj.contrast)
        program.setUniform1f("u_Saturation", adj.saturation)
        program.setUniform1f("u_Exposure", adj.exposure)
        program.setUniform1f("u_Sharpen", adj.sharpen)
        program.setUniform1f("u_Highlights", adj.highlights)
        program.setUniform1f("u_Shadows", adj.shadows)
        program.setUniform1f("u_Temperature", adj.temperature)
        program.setUniform1f("u_Tint", adj.tint)
        program.setUniform1f("u_Vignette", adj.vignette)
        program.setUniform1f("u_Fade", adj.fade)
        program.setUniform1f("u_Grain", adj.grain)
        program.setUniform2f("u_TexelSize", 1.0f / canvasW, 1.0f / canvasH)
        program.setUniform1f("u_Time", (System.currentTimeMillis() % 1000000L) / 1000.0f)

        drawQuad(program)
    }

    private fun renderTransition(pass: RenderPass.TransitionPass) {
        val program = transitionProgram ?: return
        program.use()

        bindTexture(GLES30.GL_TEXTURE0, pass.textureAId)
        program.setUniform1i("u_TextureA", 0)
        bindTexture(GLES30.GL_TEXTURE1, pass.textureBId)
        program.setUniform1i("u_TextureB", 1)

        program.setUniform1f("u_Progress", pass.progress)
        val typeInt = when (pass.transitionType) {
            TransitionType.DISSOLVE -> 0
            TransitionType.ZOOM -> 1
            TransitionType.SLIDE_LEFT -> 2
            TransitionType.SLIDE_RIGHT -> 3
            TransitionType.GLITCH -> 4
            TransitionType.BLUR -> 5
            TransitionType.WIPE_RIGHT -> 6
        }
        program.setUniform1i("u_Type", typeInt)

        drawQuad(program)
    }

    private fun bindTexture(textureUnit: Int, textureId: Int) {
        GLES30.glActiveTexture(textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
    }

    private fun drawQuad(program: GlProgram) {
        val posLoc = program.getAttribLocation("a_Position")
        val texLoc = program.getAttribLocation("a_TexCoord")

        if (posLoc >= 0) {
            GLES30.glEnableVertexAttribArray(posLoc)
            GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        }
        if (texLoc >= 0) {
            GLES30.glEnableVertexAttribArray(texLoc)
            GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        if (posLoc >= 0) GLES30.glDisableVertexAttribArray(posLoc)
        if (texLoc >= 0) GLES30.glDisableVertexAttribArray(texLoc)
    }

    private fun parseColor(hex: String): FloatArray {
        return try {
            val c = Color.parseColor(hex)
            floatArrayOf(
                Color.red(c) / 255.0f,
                Color.green(c) / 255.0f,
                Color.blue(c) / 255.0f,
                Color.alpha(c) / 255.0f
            )
        } catch (e: Exception) {
            floatArrayOf(0f, 0f, 0f, 1f)
        }
    }

    fun release() {
        canvasSolidProgram?.release()
        canvasGradientProgram?.release()
        canvasBlurProgram?.release()
        colorGradingProgram?.release()
        chromaKeyProgram?.release()
        geometricMaskProgram?.release()
        transitionProgram?.release()
    }
}
