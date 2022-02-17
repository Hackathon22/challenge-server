package systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ScreenUtils
import components.SpriteComponent
import components.TransformComponent
import core.*
import render.SpriteRegister

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
        }
        catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        assert(entities.size == 1) { "Only one camera is supported at the moment" }
        if (_cameraID != null) {
            val transformComponent = instance.getComponent<TransformComponent>(_cameraID!!)
            camera.position.set(transformComponent.pos.x, transformComponent.pos.y, transformComponent.pos.z)
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

    private var _spriteBatch : SpriteBatch? = null

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
            val region = TextureRegion(texture)
            _spriteBatch?.draw(
                region,
                transformComponent.pos.x - (texture.width.toFloat() / 2f),
                transformComponent.pos.y - (texture.height.toFloat() / 2f),
                0f,
                0f,
                texture.width.toFloat(),
                texture.height.toFloat(),
                transformComponent.scale.x,
                transformComponent.scale.y,
                transformComponent.rot.z
            )
        }

        _spriteBatch?.end()
    }

    override fun onEntityAdded(entity: Entity) {}

    override fun onEntityRemoved(entity: Entity) {}
}