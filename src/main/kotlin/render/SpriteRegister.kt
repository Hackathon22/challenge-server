package render

import com.badlogic.gdx.graphics.Texture
import parser.XMLObjectReader

const val defaultSpriteConfigurationPath = "/SpriteConfiguration.xml"

const val invalidSpriteName = "invalid"

typealias SpriteConfiguration = HashMap<String, String>

/**
 * Singleton instance that holds all the textures of the game.
 */
object SpriteRegister {

    /**
     * Wherever the textures are loaded into memory.
     */
    private var _initialized = false

    /**
     * Maps a texture for each sprite name
     */
    private var _textureMap = HashMap<String, Texture>()

    /**
     * Gets a texture from the sprite name.
     * @param name, the name of the sprite.
     */
    fun getSprite(name: String): Texture {
        assert(_initialized)
        // returns the sprite from the name. If it does not exist, return invalid sprite
        return _textureMap[name] ?: return _textureMap[invalidSpriteName]!!
    }

    /**
     * Loads all the textures in memory and prepares the texture map.
     * @param configFile, file where all the sprites textures file are mapped. Default value is [defaultSpriteConfigurationPath]
     */
    fun initialize(configFile: String = defaultSpriteConfigurationPath) {
        val configuration = XMLObjectReader.readObject<SpriteConfiguration>(configFile)
        configuration.forEach { (name, path) ->
            _textureMap[name] = Texture(path)
            if (name == "bricks") {
                _textureMap[name]?.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
            }
        }
        _initialized = true
    }
}