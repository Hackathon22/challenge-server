package core

import parser.ComponentParser
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream


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

    fun saveEntities(path: String, configuration: HashMap<String, String>) {
        val encoder = XMLEncoder(BufferedOutputStream(FileOutputStream(path)))
        encoder.writeObject(configuration)
        encoder.close()
    }

    fun getComponents(name: String): List<IComponent> {
        assert(initialized)
        val entityPath = entityConfiguration!![name]!!
        return ComponentParser.loadComponents(entityPath)
    }

}