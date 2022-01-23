package parser

import core.IComponent

/**
 * This parser will load list of components from entity configuration files.
 */
object ComponentParser {

    fun loadComponents(path: String) : ArrayList<IComponent> {
        val components = XMLObjectReader.readObject<ArrayList<IComponent>>(path)
        return convertComponentsToNative(components)
    }

    fun saveComponents(path: String, components: List<IComponent>) {
        val serializableComponents = convertComponentsToSerializable(components)
        XMLObjectWriter.writeObject(path, serializableComponents)
    }
}