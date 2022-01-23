package components

import core.IComponent
import core.NoArg
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
abstract class MovementCommand : Command()

/**
 * Called when moving is required
 */
class JumpCommand : MovementCommand()

/**
 * Called when asking to move left or right
 */
class MoveCommand(val direction : Direction) : MovementCommand() {
    enum class Direction {
        LEFT,
        RIGHT
    }
}

/**
 * Called upon moving release
 */
class StopCommand : MovementCommand()

@NoArg
data class CommandComponent(val controllerType: ControllerType,
                            val commands: Queue<Command> = LinkedList()) : IComponent
