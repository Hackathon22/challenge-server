package core

import components.NameComponent
import components.TransformComponent
import parser.ComponentParser
import java.beans.XMLDecoder
import java.io.BufferedInputStream
import java.io.FileInputStream


/**
 * This object contains all the methods to create entities from names.
 */
object EntityFactory {

    private const val configurationFile = "src/resources/EntityConfiguration.xml"

    private var entityConfiguration :HashMap<String, String>? = null

    private var initialized = false

    fun loadEntities(path: String = configurationFile) {
        val parser = XMLDecoder(BufferedInputStream(FileInputStream(path)))
        entityConfiguration = parser.readObject() as HashMap<String, String>
        initialized = true
    }

    fun getComponents(name: String) : List<IComponent> {
        assert(initialized)
        val entityPath = entityConfiguration!![name]!!
        val loadedComponents = ComponentParser.loadComponents(entityPath)
        loadedComponents.add(NameComponent(name))
        return loadedComponents
    }

}