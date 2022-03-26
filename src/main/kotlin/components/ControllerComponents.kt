package components

import com.google.gson.annotations.SerializedName
import core.IComponent
import core.NoArg
import core.Vec3F
import systems.JSONConvertable
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
abstract class StateCommand(@SerializedName("commandType") val commandType: String)
    : Command(), JSONConvertable

/**
 * Called when asking to move left or right
 */
class MoveCommand(@SerializedName("direction") val direction : Vec3F,
                  @SerializedName("release") val release: Boolean = false) : StateCommand("moveCommand") {
    override fun toString(): String {
        return "MoveCommand(x=${direction.x}, y=${direction.y}, z=${direction.z}"
    }
}

class CursorMovedCommand(val worldPosition: Vec3F) : Command()

/**
 * Called when asking to shoot
 */
class ShootCommand(@SerializedName("angle") val angle: Float? = null) : StateCommand("shootCommand") {
    override fun toString(): String {
        return "ShootCommand(angle=$angle)"
    }
}


data class CommandComponent(var controllerType: ControllerType = ControllerType.LOCAL_INPUT,
                            val commands: Queue<Command> = LinkedList()) : IComponent
