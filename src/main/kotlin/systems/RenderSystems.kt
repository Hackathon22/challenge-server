package systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import components.*
import core.*
import render.SpriteRegister
import java.lang.StringBuilder

class CameraSystem : System() {

    val camera = OrthographicCamera()

    private var _cameraID: Entity? = null


    /**
     * Initializes the LibGDX camera.
     * @param arg, Expects the following arguments
     *      - width, the width of the window as Integer
     *      - height, the height of the window as Integer
     */
    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.size < 2) return false
        return try {
            val width = arg[0] as Int
            val height = arg[1] as Int
            camera.setToOrtho(false, width.toFloat(), height.toFloat())
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        assert(entities.size == 1) { "Only one camera is supported at the moment" }
        if (_cameraID != null) {
            val transformComponent = instance.getComponent<TransformComponent>(_cameraID!!)
            camera.position.set(
                transformComponent.pos.x,
                transformComponent.pos.y,
                transformComponent.pos.z
            )
        }
        camera.update()
    }

    override fun onEntityAdded(entity: Entity) {
//        assert(_cameraID != null) { "Only one camera is supported at the moment " }
        _cameraID = entity
    }

    override fun onEntityRemoved(entity: Entity) {
        if (_cameraID == entity) {
            _cameraID = null
        }
    }

    override fun onEvent(event: Event, observable: IObservable, instance: Instance) {
        if (event is WindowResizeEvent) {
            camera.setToOrtho(false, event.newSize.x, event.newSize.y)
        }
    }
}

class SpriteRenderSystem : System() {

    private var _spriteBatch: SpriteBatch? = null

    /**
     * Initializes the render system. Returns true if the given parameters are accurate.
     */
    override fun initializeLogic(vararg arg: Any): Boolean {
        _spriteBatch = SpriteBatch()
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _spriteBatch?.begin()
        val cameraReference = instance.getSystem<CameraSystem>().camera
        _spriteBatch?.projectionMatrix = cameraReference.combined

        ScreenUtils.clear(0.0f, 0.0f, 0.0f, 1.0f)

        entities.forEach {
            val spriteComponent = instance.getComponent<SpriteComponent>(it)
            val transformComponent = instance.getComponent<TransformComponent>(it)
            val texture = SpriteRegister.getSprite(spriteComponent.sprite)

            if (spriteComponent.repeat) {
                val totalWidth = (transformComponent.scale.x * texture.width).toInt()
                val totalHeight = (transformComponent.scale.y * texture.height).toInt()
                _spriteBatch?.draw(
                    texture,
                    transformComponent.pos.x - totalWidth / 2,
                    transformComponent.pos.y - totalHeight / 2,
                    0,
                    0,
                    (transformComponent.scale.x * texture.width).toInt(),
                    (transformComponent.scale.y * texture.height).toInt()
                )
            } else {
                val sprite = Sprite(texture)
                val totalWidth = texture.width.toFloat() * transformComponent.scale.x
                val totalHeight = texture.height.toFloat() * transformComponent.scale.y
                sprite.setOrigin(totalWidth / 2f, totalHeight / 2f)
                sprite.rotate(transformComponent.rot.z)
                sprite.setPosition(
                    transformComponent.pos.x - totalWidth / 2f,
                    transformComponent.pos.y - totalHeight / 2f
                )
                sprite.setScale(transformComponent.scale.x, transformComponent.scale.y)
                sprite.draw(_spriteBatch)
            }
        }

        _spriteBatch?.end()
    }

    override fun onEntityAdded(entity: Entity) {}

    override fun onEntityRemoved(entity: Entity) {}
}


class UISystem : System() {

    private var _spriteBatch: SpriteBatch? = null
    private var _font: BitmapFont? = null

    override fun initializeLogic(vararg arg: Any): Boolean {
        _spriteBatch = SpriteBatch()
        _font = BitmapFont()
        _font?.data?.setScale(1.5f)
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _spriteBatch?.begin()

        // score entities
        var counter = 0
        entities.forEach {
            val scoreComponent = instance.getComponent<ScoreComponent>(it)
            val characterComponent =
                instance.getComponentDynamicUnsafe(it, CharacterComponent::class)
            val transformComponent = instance.getComponent<TransformComponent>(it)
            val builder = StringBuilder()

            val x = if (counter == 0) 10f else Gdx.graphics.width - 270f

            // printing score and health
            builder.append("Player: ${scoreComponent.username} - Score: %.2f".format(scoreComponent.score))
            _font?.draw(_spriteBatch!!, builder.toString(), x, Gdx.graphics.height - 20f)
            builder.clear()
            if (characterComponent != null) {
                _font?.color = Color.GREEN
                builder.append("Health: ${(characterComponent as CharacterComponent).health}")
                _font?.draw(_spriteBatch!!, builder.toString(), x, Gdx.graphics.height - 50f)
                _font?.color = Color.WHITE
            }

            counter += 1
        }

        _spriteBatch?.end()
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

}