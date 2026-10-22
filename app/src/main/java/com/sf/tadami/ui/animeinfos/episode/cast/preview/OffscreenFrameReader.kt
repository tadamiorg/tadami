package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.util.Log
import android.view.Surface
import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FRAME_WIDTH_PX
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * A video surface no one sees: the decoder renders into [surface] (a SurfaceTexture on an external GL
 * texture), and [read] draws the latest frame downscaled into an offscreen pbuffer and reads it back
 * as a bitmap. The GPU converts and scales; each output pixel averages a 4×4 grid of the source so
 * a 1080p frame shrunk to 256 px doesn't alias.
 *
 * Owned by one thread (the one that called [create]): the GL context is current there, and the frame
 * callback runs there.
 */
internal class OffscreenFrameReader private constructor(
    private val display: EGLDisplay,
    private val context: EGLContext,
    private val pbuffer: EGLSurface,
    private val program: Int,
    private val texture: Int,
    private val surfaceTexture: SurfaceTexture,
    val surface: Surface,
) {
    private val transform = FloatArray(16)
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(QUAD.size * 4).order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply { put(QUAD).position(0) }
    private val pixels: ByteBuffer = ByteBuffer.allocateDirect(MAX_SIZE * MAX_SIZE * 4).order(ByteOrder.nativeOrder())

    /** Takes the decoder's latest frame as the texture's content; call on each frame. */
    fun latch() {
        surfaceTexture.updateTexImage()
    }

    /** The latched frame scaled to [width]×[height] (at most [MAX_SIZE] each), upright. */
    fun read(width: Int, height: Int): Bitmap {
        require(width in 1..MAX_SIZE && height in 1..MAX_SIZE)
        surfaceTexture.getTransformMatrix(transform)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTexMatrix"), 1, false, transform, 0)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uPixel"), 1f / width, 1f / height)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        quad.position(0)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(position)
        quad.position(2)
        GLES20.glVertexAttribPointer(texCoord, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(texCoord)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        pixels.position(0)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        checkGl("read back")
        pixels.position(0)
        // The quad is drawn upside down, so the rows read bottom-up land top-down in the bitmap
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { copyPixelsFromBuffer(pixels) }
    }

    fun release() {
        surface.release()
        surfaceTexture.release()
        GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        GLES20.glDeleteProgram(program)
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, pbuffer)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }

    companion object {
        private const val TAG = "OffscreenFrameReader"

        /** Largest frame side: the pbuffer is this square. */
        const val MAX_SIZE = PREVIEW_FRAME_WIDTH_PX

        // x, y, u, v: texture top (v = 1) at the bottom of the pbuffer, see read
        private val QUAD = floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f,
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        // 4×4 samples across the output pixel's footprint, in texture space before its transform
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            uniform mat4 uTexMatrix;
            uniform vec2 uPixel;
            void main() {
                vec4 sum = vec4(0.0);
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        vec2 offset = (vec2(float(i), float(j)) + 0.5) / 4.0 - 0.5;
                        vec2 uv = (uTexMatrix * vec4(vTexCoord + offset * uPixel, 0.0, 1.0)).xy;
                        sum += texture2D(sTexture, uv);
                    }
                }
                gl_FragColor = vec4((sum / 16.0).rgb, 1.0);
            }
        """

        /**
         * A reader on the calling thread, its frame callback [onFrame] posted to [handler] (that
         * thread's); null when the device can't make an offscreen GL surface.
         */
        fun create(handler: Handler, onFrame: () -> Unit): OffscreenFrameReader? {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
            var context: EGLContext = EGL14.EGL_NO_CONTEXT
            var pbuffer: EGLSurface = EGL14.EGL_NO_SURFACE
            try {
                val config = chooseConfig(display) ?: error("no RGBA8888 ES2 pbuffer config")
                context = EGL14.eglCreateContext(
                    display, config, EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
                )
                if (context == EGL14.EGL_NO_CONTEXT) error("eglCreateContext failed")
                pbuffer = EGL14.eglCreatePbufferSurface(
                    display, config, intArrayOf(EGL14.EGL_WIDTH, MAX_SIZE, EGL14.EGL_HEIGHT, MAX_SIZE, EGL14.EGL_NONE), 0,
                )
                if (pbuffer == EGL14.EGL_NO_SURFACE) error("eglCreatePbufferSurface failed")
                if (!EGL14.eglMakeCurrent(display, pbuffer, pbuffer, context)) error("eglMakeCurrent failed")

                val program = buildProgram()
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                val texture = textures[0]
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                checkGl("texture")
                val surfaceTexture = SurfaceTexture(texture)
                surfaceTexture.setOnFrameAvailableListener({ onFrame() }, handler)
                return OffscreenFrameReader(display, context, pbuffer, program, texture, surfaceTexture, Surface(surfaceTexture))
            } catch (e: Exception) {
                Log.w(TAG, "No offscreen GL surface for the cast seekbar preview", e)
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (pbuffer != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, pbuffer)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
                return null
            }
        }

        private fun chooseConfig(display: EGLDisplay): EGLConfig? {
            val attributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) || count[0] == 0) return null
            return configs[0]
        }

        private fun buildProgram(): Int {
            val vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertex)
            GLES20.glAttachShader(program, fragment)
            GLES20.glLinkProgram(program)
            val linked = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
            GLES20.glDeleteShader(vertex)
            GLES20.glDeleteShader(fragment)
            if (linked[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                error("program link failed: $log")
            }
            return program
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source.trimIndent())
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("shader compile failed: $log")
            }
            return shader
        }

        private fun checkGl(step: String) {
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) error("GL error 0x${Integer.toHexString(error)} at $step")
        }
    }
}
