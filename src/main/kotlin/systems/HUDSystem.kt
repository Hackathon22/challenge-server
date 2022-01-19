package systems

import core.Entity
import core.Instance
import core.System
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL

class HUDSystem : System() {

    private var _width = 0
    private var _height = 0

    private var _window = 0L

    override fun onEntityAdded(entity: Entity) {}

    override fun onEntityRemoved(entity: Entity) {}

    /**
     * Initializes the system parameters.
     * @param arg an array of any type of object expecting the width and height of the window consecutively, both
     * parameters are expected to be Integers.
     */
    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.size != 2) return false
        return try {
            _width = arg[0] as Int
            _height = arg[1] as Int
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        GL.createCapabilities()
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
        while (!glfwWindowShouldClose(_window)) {
            glClear(GL_COLOR_BUFFER_BIT.or(GL_DEPTH_BUFFER_BIT))
            glfwSwapBuffers(_window)
            glfwPollEvents()
        }
    }

    /**
     * Opens the window and initializes the system
     */
    fun start() {
        // error callback: will print the erros
        GLFWErrorCallback.createPrint(java.lang.System.err).set()

        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        // optional as they are already set as default
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        _window = glfwCreateWindow(_width, _height, "ARSWA", NULL, NULL)
        if (_window == NULL) {
            throw RuntimeException("Unable to create GLFW window")
        }

        val stack = stackPush()
        val pWidth = stack.mallocInt(1)
        val pHeight = stack.mallocInt(1)

        // get the window size
        glfwGetWindowSize(_window, pWidth, pHeight)

        // get the resolution of the primary monitor
        val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

        // center the window
        glfwSetWindowPos(_window, (vidMode.width() - pWidth.get(0)) / 2, (vidMode.height() - pHeight.get(0) / 2))

        glfwMakeContextCurrent(_window)

        glfwSwapInterval(1)

        glfwShowWindow(_window)
    }

    fun dispose() {

    }
}