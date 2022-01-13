package core

import java.lang.RuntimeException

operator fun Number.minus(other: Number): Number {
    return when (this) {
        is Long -> this.toLong() - other.toLong()
        is Int -> this.toInt() - other.toInt()
        is Short -> this.toShort() - other.toShort()
        is Byte -> this.toByte() - other.toByte()
        is Double -> this.toDouble() - other.toDouble()
        is Float -> this.toFloat() - other.toFloat()
        else -> throw RuntimeException("Unknown numeric type in minus operation")
    }
}

operator fun Number.plus(other: Number): Number {
    return when (this) {
        is Long -> this.toLong() + other.toLong()
        is Int -> this.toInt() + other.toInt()
        is Short -> this.toShort() + other.toShort()
        is Byte -> this.toByte() + other.toByte()
        is Double -> this.toDouble() + other.toDouble()
        is Float -> this.toFloat() + other.toFloat()
        else -> throw RuntimeException("Unknown numeric type in minus operation")
    }
}

operator fun Number.times(other: Number): Number {
    return when (this) {
        is Long -> this.toLong() * other.toLong()
        is Int -> this.toInt() * other.toInt()
        is Short -> this.toShort() * other.toShort()
        is Byte -> this.toByte() * other.toByte()
        is Double -> this.toDouble() * other.toDouble()
        is Float -> this.toFloat() * other.toFloat()
        else -> throw RuntimeException("Unknown numeric type in minus operation")
    }
}

operator fun Number.div(other: Number): Number {
    return when (this) {
        is Long -> this.toLong() / other.toLong()
        is Int -> this.toInt() / other.toInt()
        is Short -> this.toShort() / other.toShort()
        is Byte -> this.toByte() / other.toByte()
        is Double -> this.toDouble() / other.toDouble()
        is Float -> this.toFloat() / other.toFloat()
        else -> throw RuntimeException("Unknown numeric type in minus operation")
    }
}


data class Vec2(var x: Number, var y: Number) {

    init {
        assert(x::class == y::class)
    }

    operator fun plus(vec: Vec2): Vec2 {
        assert(x::class == vec.x::class)
        return Vec2(x + vec.x, y + vec.y)
    }

    operator fun minus(vec: Vec2): Vec2 {
        assert(x::class == vec.x::class)
        return Vec2(x - vec.x, y - vec.y)
    }

    operator fun div(vec: Vec2): Vec2 {
        assert(x::class == vec.x::class)
        return Vec2(x / vec.x, y / vec.y)
    }

    operator fun times(vec: Vec2): Vec2 {
        assert(x::class == vec.x::class)
        return Vec2(x * vec.x, y * vec.y)
    }

    override fun toString(): String {
        return "Vec2{x=$x, y=$y}"
    }
}

data class Vec3(var x: Number, var y: Number, var z: Number) {

    init {
        assert(x::class == y::class && y::class == z::class)
    }

    operator fun plus(vec: Vec3): Vec3 {
        assert(x::class == vec.x::class)
        return Vec3(x + vec.x, y + vec.y, z + vec.z)
    }

    operator fun minus(vec: Vec3): Vec3 {
        assert(x::class == vec.x::class)
        return Vec3(x - vec.x, y - vec.y, z - vec.z)
    }

    operator fun times(vec: Vec3): Vec3 {
        assert(x::class == vec.x::class)
        return Vec3(x * vec.x, y * vec.y, z * vec.z)
    }

    operator fun times(scalar: Number): Vec3 {
        return Vec3(x * scalar, y * scalar, z * scalar)
    }

    operator fun div(vec: Vec3): Vec3 {
        assert(x::class == vec.x::class)
        return Vec3(x / vec.x, y / vec.y, z / vec.z)
    }

    override fun toString(): String {
        return "Vec3{x=$x, y=$y, z=$z}"
    }
}
