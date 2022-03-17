package components

import core.IComponent

data class CharacterComponent(var health : Float = 20.0f, val maxHealth : Float = health, val maxSpeed : Float = 100.0f, val deathTime : Float = 5.0f) : IComponent
