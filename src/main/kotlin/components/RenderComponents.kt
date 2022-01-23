package components

import core.IComponent
import core.Vec2F

class CameraComponent : IComponent

class SpriteComponent(var sprite: String = "invalid", val scale: Vec2F = Vec2F(1f, 1f)) : IComponent

