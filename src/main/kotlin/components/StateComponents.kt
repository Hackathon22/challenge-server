package components

import core.IComponent
import java.util.*

enum class State {
    WALKING,
    JUMPING,
    MELEE_ATTACK,
    CASTING,
    STUNNED,
    ROOTED
}

data class StateComponent(val states: Stack<State> = Stack()) : IComponent
