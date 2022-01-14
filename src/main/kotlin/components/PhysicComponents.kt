package components

import core.IComponent
import core.Vec3

data class TransformComponent(var pos: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                              var rot: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                              var scale: Vec3 = Vec3(1.0f, 1.0f, 1.0f)
) : IComponent

data class DynamicComponent(var speed: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                            var acceleration: Vec3 = Vec3(0.0f, 0.0f, 0.0f)
) : IComponent


