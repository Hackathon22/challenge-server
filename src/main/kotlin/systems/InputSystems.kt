package systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import components.*
import core.Entity
import core.Instance
import core.System
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

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.UP) {
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) _queue.add(StopCommand())
            else _queue.add(MoveCommand(MoveCommand.Direction.UP))
        }
        else if (keycode == Input.Keys.DOWN) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) _queue.add(StopCommand())
            else _queue.add(MoveCommand(MoveCommand.Direction.DOWN))
        }
        else if (keycode == Input.Keys.LEFT) {
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) _queue.add(StopCommand())
            else _queue.add(MoveCommand(MoveCommand.Direction.LEFT))
        }
        else if (keycode == Input.Keys.RIGHT) {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) _queue.add(StopCommand())
            else _queue.add(MoveCommand(MoveCommand.Direction.RIGHT))
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Input.Keys.UP) {
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) _queue.add(MoveCommand(MoveCommand.Direction.DOWN))
            else _queue.add(StopCommand())
        }
        else if (keycode == Input.Keys.DOWN) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) _queue.add(MoveCommand(MoveCommand.Direction.UP))
            else _queue.add(StopCommand())
        }
        else if (keycode == Input.Keys.LEFT) {
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) _queue.add(MoveCommand(MoveCommand.Direction.RIGHT))
            else _queue.add(StopCommand())
        }
        else if (keycode == Input.Keys.RIGHT) {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) _queue.add(MoveCommand(MoveCommand.Direction.LEFT))
            else _queue.add(StopCommand())
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