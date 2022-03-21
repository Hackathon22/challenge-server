package components

import core.IComponent
import core.Vec3F
import java.util.*

/**
 * Which kind on controller has impact on the command component?
 */
enum class ControllerType {
    LOCAL_INPUT,    // client that controls its character for example
    AI,             // algorithm that controls an entity
    NETWORK         // if a command is passed through network, although is it better to synchronize state from
                    // component values directly
}

/**
 * Base class for all commands.
 * A command object holds information from its type and can hold additional information (angle, text, etc).
 */
abstract class Command

/**
 * Movement related commands, such as jumping; going left, right, stop etc...
 */
abstract class StateCommand : Command()

/**
 * Called when moving is required
 */
class JumpCommand : StateCommand()

/**
 * Called when asking to move left or right
 */
class MoveCommand(val direction : Vec3F, val release: Boolean = false) : StateCommand()

class CursorMovedCommand(val worldPosition: Vec3F) : Command()

/**
 * Called when asking to shoot
 */
class ShootCommand(val angle: Float? = null) : StateCommand() {
}


data class CommandComponent(var controllerType: ControllerType = ControllerType.LOCAL_INPUT,
                            val commands: Queue<Command> = LinkedList()) : IComponent
