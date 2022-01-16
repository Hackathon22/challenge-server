package parser

import core.IComponent
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * This parser will load list of components from entity configuration files.
 */
object ComponentParser {

    fun loadComponents(path: String) : ArrayList<IComponent> {
        val decoder = XMLDecoder(BufferedInputStream(FileInputStream(path)))
        val components = decoder.readObject() as ArrayList<IComponent>

        val nativeComponents = convertComponentsToNative(components)

        decoder.close()
        return nativeComponents
    }

    fun saveComponents(path: String, components: List<IComponent>) {
        val encoder = XMLEncoder(BufferedOutputStream(FileOutputStream(path)))

        val serializableComponents = convertComponentsToSerializable(components)

        encoder.writeObject(serializableComponents)
        encoder.close()
    }
}