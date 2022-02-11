package core

import kotlin.math.sqrt

data class Vec2I(var x: Int, var y: Int) {

    operator fun plus(vec: Vec2I) = Vec2I(x + vec.x, y + vec.y)

    operator fun minus(vec: Vec2I) = Vec2I(x - vec.x, y - vec.y)

    operator fun div(vec: Vec2I) = Vec2I(x / vec.x, y / vec.y)

    operator fun times(vec: Vec2I) = Vec2I(x * vec.x, y * vec.y)

    operator fun times(scalar: Float) = Vec2I((x * scalar).toInt(), (y * scalar).toInt())
}

data class Vec2F(var x: Float, var y: Float) {

    operator fun plus(vec: Vec2F) = Vec2F(x + vec.x, y + vec.y)

    operator fun minus(vec: Vec2F) = Vec2F(x - vec.x, y - vec.y)

    operator fun div(vec: Vec2F) = Vec2F(x / vec.x, y / vec.y)

    operator fun times(vec: Vec2F) = Vec2F(x * vec.x, y * vec.y)

    operator fun times(scalar: Float) = Vec2F(x * scalar, y * scalar)
}

data class Vec3I(var x: Int, var y: Int, var z: Int) {

    operator fun plus(vec: Vec3I) = Vec3I(x + vec.x, y + vec.y, z + vec.z)

    operator fun minus(vec: Vec3I) =  Vec3I(x - vec.x, y - vec.y, z - vec.z)

    operator fun div(vec: Vec3I) = Vec3I(x / vec.x, y / vec.y, z / vec.z)

    operator fun times(vec: Vec3I) = Vec3I(x * vec.x, y * vec.y, z * vec.z)

    operator fun times(scalar: Float) = Vec3I((x * scalar).toInt(), (y * scalar).toInt(), (z * scalar).toInt())

}

data class Vec3F(var x: Float, var y: Float, var z: Float) {

    operator fun plus(vec: Vec3F) = Vec3F(x + vec.x, y + vec.y, z + vec.z)

    operator fun minus(vec: Vec3F) = Vec3F(x - vec.x, y - vec.y, z - vec.z)

    operator fun div(vec: Vec3F) = Vec3F(x / vec.x, y / vec.y, z / vec.z)

    operator fun times(vec: Vec3F) = Vec3F(x * vec.x, y * vec.y, y * vec.y)

    operator fun times(scalar: Float) = Vec3F(x * scalar, y * scalar, z * scalar)

    fun normalized() : Vec3F {
        val norm = sqrt(x * x + y * y + z * z)
        return if (norm != 0f) Vec3F(x / norm, y / norm, z / norm) else Vec3F(0f, 0f, 0f)
    }
}
