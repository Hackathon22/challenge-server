package components

import core.IComponent
import java.util.*

enum class States {
    IDLE,
    MOVING,
    SHOOTING,
    HIT,
    DEAD
}

data class StateComponent(var state : States = States.IDLE) : IComponent
