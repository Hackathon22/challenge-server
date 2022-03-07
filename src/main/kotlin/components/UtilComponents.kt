package components

import core.IComponent

data class TimerComponent(val maxTime: Float, var currentTime: Float = maxTime) : IComponent
