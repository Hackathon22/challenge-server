package render

import com.badlogic.gdx.graphics.Texture

object TextureManager {

    private var _initialized = false

    private var _textureMap = HashMap<String, Texture>()

    fun getTexture(name: String) : Texture {
        assert(_initialized)
        assert(_textureMap[name] != null)
        // TODO "if null set default texture"
        return _textureMap[name]!!
    }

    fun initialize(configFile: String) {
        //TODO("Write this initialization)
        _initialized = true
    }

}