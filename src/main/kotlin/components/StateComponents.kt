package components

import core.IComponent
import java.util.*

enum class States {
    IDLE,
    MOVING,
    SHOOTING,
    HIT
}

data class StateComponent(var state : States = States.IDLE) : IComponent
