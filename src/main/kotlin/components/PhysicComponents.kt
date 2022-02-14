package components

import core.IComponent
import core.Vec3F

data class TransformComponent(var pos: Vec3F = Vec3F(0.0f, 0.0f, 0.0f),
                              var rot: Vec3F = Vec3F(0.0f, 0.0f, 0.0f),
                              var scale: Vec3F = Vec3F(1.0f, 1.0f, 1.0f)
) : IComponent

data class DynamicComponent(var speed: Vec3F = Vec3F(0.0f, 0.0f, 0.0f),
                            var acceleration: Vec3F = Vec3F(0.0f, 0.0f, 0.0f),
                            var gravity: Float = 0.0f
) : IComponent

data class BodyComponent(var width: Float, var height: Float)