package systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import components.*
import core.*
import java.util.concurrent.LinkedBlockingQueue

class InputSystem : System(), InputProcessor {

    private val _queue = LinkedBlockingQueue<Command>()

    override fun initializeLogic(vararg arg: Any): Boolean {
        Gdx.input.inputProcessor = this
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        // adds the commands to each component if they match the controller type
        entities.forEach {
            val commandComponent = instance.getComponent<CommandComponent>(it)
            if (commandComponent.controllerType == ControllerType.LOCAL_INPUT) {
                commandComponent.commands.addAll(_queue)
            }
        }
        // clears the commands
        _queue.clear()
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    @Suppress("DuplicatedCode")
    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP -> {
                _queue.add(MoveCommand(Vec3F(0f, 1f, 0f)))
            }
            Input.Keys.DOWN -> {
                _queue.add(MoveCommand(Vec3F(0f, -1f, 0f)))
            }
            Input.Keys.LEFT -> {
                _queue.add(MoveCommand(Vec3F(-1f, 0f, 0f)))
            }
            Input.Keys.RIGHT -> {
                _queue.add(MoveCommand(Vec3F(1f, 0f, 0f)))
            }
        }
        return true
    }

    @Suppress("DuplicatedCode")
    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP -> {
                _queue.add(MoveCommand(Vec3F(0f, -1f, 0f)))
            }
            Input.Keys.DOWN -> {
                _queue.add(MoveCommand(Vec3F(0f, 1f, 0f)))
            }
            Input.Keys.LEFT -> {
                _queue.add(MoveCommand(Vec3F(1f, 0f, 0f)))
            }
            Input.Keys.RIGHT -> {
                _queue.add(MoveCommand(Vec3F(-1f, 0f, 0f)))
            }
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean = true

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = true

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = true

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = true

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = true

    override fun scrolled(amountX: Float, amountY: Float): Boolean = true

}