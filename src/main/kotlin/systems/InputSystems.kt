package systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector3
import components.*
import core.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class InputSystem : System(), InputProcessor {

    private val _queue = LinkedBlockingQueue<Command>()

    private val _mouseQueue = LinkedBlockingQueue<Pair<Int, Int>>()

    override fun initializeLogic(vararg arg: Any): Boolean {
        Gdx.input.inputProcessor = this
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        // generates commands from the mouse queue
        _mouseQueue.forEach {
            // get the camera system in order to obtain the real world cursor position
            val cameraSystem = instance.getSystem<CameraSystem>()
            val worldCoordinates = cameraSystem.camera.unproject(
                Vector3(
                    it.first.toFloat(),
                    it.second.toFloat(),
                    0.0f
                )
            )
            val cursorMoved = CursorMovedCommand(
                Vec3F(
                    worldCoordinates.x,
                    worldCoordinates.y,
                    worldCoordinates.z
                )
            )
            // adds the new cursor moved command
            _queue.add(cursorMoved)
        }

        // adds the commands to each component if they match the controller type
        entities.forEach {
            val commandComponent = instance.getComponent<CommandComponent>(it)
            if (commandComponent.controllerType == ControllerType.LOCAL_INPUT) {
                commandComponent.commands.addAll(_queue)
            }
        }
        // clears the commands
        _queue.clear()
        _mouseQueue.clear()
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
            Input.Keys.SPACE -> {
                _queue.add(ShootCommand())
            }
        }
        return true
    }

    @Suppress("DuplicatedCode")
    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP -> {
                _queue.add(MoveCommand(Vec3F(0f, -1f, 0f), release = true))
            }
            Input.Keys.DOWN -> {
                _queue.add(MoveCommand(Vec3F(0f, 1f, 0f), release = true))
            }
            Input.Keys.LEFT -> {
                _queue.add(MoveCommand(Vec3F(1f, 0f, 0f), release = true))
            }
            Input.Keys.RIGHT -> {
                _queue.add(MoveCommand(Vec3F(-1f, 0f, 0f), release = true))
            }
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean = true

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        _queue.add(ShootCommand())
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = true

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = true

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        _mouseQueue.add(Pair(screenX, screenY))
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean = true

}