package renderGL

import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Holds an OPENGL shader.
 */
class Shader {
    var initialized = false
        private set

    var id = 0
        private set

    fun initialize(path: String, debug: Boolean = false) : Boolean {
        // loads the shader
        val vertexShader =
            Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)

        // creates it and compiles with OpenGL
        id = glCreateShader(GL20.GL_VERTEX_SHADER)
        glShaderSource(id, vertexShader as CharSequence)
        glCompileShader(id)

        val stack = stackPush()
        val pSuccess = stack.mallocInt(1)
        glGetShaderiv(id, GL_COMPILE_STATUS, pSuccess)

        initialized = if (pSuccess.get() == 0) {
            // gets the compilation error if debug mode in on
            if (debug) {
                val shaderInfo = glGetShaderInfoLog(id)
                System.err.println(shaderInfo)
            }
            false
        } else true
        return initialized
    }

    fun dispose() {
        glDeleteShader(id)
        id = 0
        initialized = false
    }
}


class ShaderProgram {
    private var _shaders = ArrayList<Shader>()
    private var _id = 0

    var linked = false
        private set

    init {
        _id = glCreateProgram()
    }

    fun link(debug: Boolean = false) : Boolean {
        glLinkProgram(_id)
        val stack = stackPush()
        val pSuccess = stack.mallocInt(1)
        glGetProgramiv(_id, GL_LINK_STATUS, pSuccess)
        linked = if (pSuccess.get() == 0) {
            if (debug) {
                val programInfo = glGetProgramInfoLog(_id)
                System.err.println(programInfo)
            }
            false
        } else true

        return linked
    }

    fun use() {
        assert(linked)
        glUseProgram(_id)
    }

    fun addShader(shader: Shader) {
        _shaders.add(shader)
        glAttachShader(_id, shader.id)
    }

    fun removeShader(shader: Shader) {
        glDetachShader(_id, shader.id)
        _shaders.remove(shader)
    }

    fun dispose() {
        _shaders.forEach {
            glDetachShader(_id, it.id)
        }
        _shaders.clear()
        glDeleteProgram(_id)
        linked = false
    }

}