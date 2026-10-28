package com.procut.render

import android.opengl.GLES30
import android.util.Log

class GlProgram(vertexSource: String, fragmentSource: String) {

    val programId: Int
    private val uniformLocations = mutableMapOf<String, Int>()
    private val attributeLocations = mutableMapOf<String, Int>()

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

        programId = GLES30.glCreateProgram()
        check(programId != 0) { "Failed to create OpenGL program" }

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Program link failed: $log")
        }

        // Shaders can be deleted once linked
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            GLES30.glGetUniformLocation(programId, name)
        }
    }

    fun getAttribLocation(name: String): Int {
        return attributeLocations.getOrPut(name) {
            GLES30.glGetAttribLocation(programId, name)
        }
    }

    fun setUniformMatrix4fv(name: String, matrix: Matrix4f) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniformMatrix4fv(location, 1, false, matrix.values, 0)
        }
    }

    fun setUniform1f(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) GLES30.glUniform1f(location, value)
    }

    fun setUniform1i(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location >= 0) GLES30.glUniform1i(location, value)
    }

    fun setUniform2f(name: String, x: Float, y: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) GLES30.glUniform2f(location, x, y)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) GLES30.glUniform3f(location, x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) GLES30.glUniform4f(location, x, y, z, w)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
        }
    }

    companion object {
        private const val TAG = "GlProgram"

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            check(shader != 0) { "Failed to create shader type $type" }

            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val infoLog = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Shader compilation failed (type $type):\n$infoLog\nSource:\n$source")
            }
            return shader
        }
    }
}
