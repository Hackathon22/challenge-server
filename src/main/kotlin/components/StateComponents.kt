package components

import core.IComponent

enum class States {
    IDLE,
    MOVING,
    SHOOTING,
    HIT,
    DEAD
}

data class StateComponent(var state : States = States.IDLE) : IComponent
