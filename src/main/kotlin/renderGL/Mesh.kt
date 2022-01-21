package renderGL

import org.lwjgl.opengl.GL15.*
import org.lwjgl.system.MemoryStack.stackPush

class Mesh {
    private var _initialized = false
    private var _VBO = 0

    fun initialize(array: FloatArray) : Boolean {
        val stack = stackPush()
        val pVBO = stack.mallocInt(1)

        glGenBuffers(pVBO)
        glBindBuffer(GL_ARRAY_BUFFER, pVBO.get())
        glBufferData(GL_ARRAY_BUFFER, array, GL_STATIC_DRAW)
        _initialized = true

        return _initialized
    }

}