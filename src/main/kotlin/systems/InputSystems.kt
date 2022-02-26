package systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector3
import components.*
import core.*
import java.util.concurrent.LinkedBlockingQueue

class InputSystem : System(), InputProcessor {

    private val _queue = LinkedBlockingQueue<Command>()

    private val _mouseQueue = LinkedBlockingQueue<Pair<Int, Int>>()

    private var _lastDirection = Vec3F(0f, 0f, 0f)

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

        // get direction (easier to handle in the loop than from events)
        val currentDirection = processDirectionKey()
        _queue.add(MoveCommand(currentDirection))

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
            Input.Keys.SPACE -> {
                _queue.add(ShootCommand())
            }
        }
        return true
    }

    @Suppress("DuplicatedCode")
    override fun keyUp(keycode: Int): Boolean {
        return true
    }

    private fun processDirectionKey() : Vec3F {
        val direction = Vec3F(0f, 0f, 0f)
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            direction.y += 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            direction.y -= 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            direction.x -= 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            direction.x += 1f
        }
        return direction
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