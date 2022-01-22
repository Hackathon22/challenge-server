package components

import com.badlogic.gdx.math.Vector2
import core.IComponent
import core.Vec2F

data class CameraComponent(val position: Vec2F) : IComponent

data class SpriteComponent(val sprite: String, val scale: Vector2 = Vector2(1f, 1f)) : IComponent
