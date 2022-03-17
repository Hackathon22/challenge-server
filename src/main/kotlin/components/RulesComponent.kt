package components

import core.IComponent

data class ZoneComponent(val width : Float, val height : Float) : IComponent

data class ScoreComponent(var team: Int = 0, var score: Float = 0f, var gameOn : Boolean = true, val username: String = "player") : IComponent

data class SpawnerComponent(var team: Int = 0) : IComponent
