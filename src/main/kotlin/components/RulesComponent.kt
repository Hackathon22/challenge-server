package components

import core.IComponent

data class ZoneComponent(val width : Float, val height : Float) : IComponent

data class ScoreComponent(var score: Float = 0f, val username: String = "player") : IComponent
