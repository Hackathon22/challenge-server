package parser

import components.DynamicComponent
import components.TransformComponent
import components.NetworkComponent
import core.Entity
import core.IComponent
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

typealias Scene = HashMap<Entity, ArrayList<IComponent>>

private val componentNames = hashMapOf(
    "transformComponent" to TransformComponent::class,
    "dynamicComponent" to DynamicComponent::class
)

private val inverseComponentNames = componentNames.entries.associate { (k, v) -> v to k }

private fun parseNetworkComponent(component: SerializableNetworkComponent): NetworkComponent {
    val parsedComponent = NetworkComponent()
    parsedComponent.clientPredicted = component.clientPredicted
    parsedComponent.ignorePrediction = component.ignorePrediction
    component.synchronizedProperties.forEach { (comp, properties) ->
        val componentClass = componentNames[comp]!!
        parsedComponent.synchronizedProperties[componentClass] = properties
    }
    return parsedComponent
}

private fun parseSerializableNetworkComponent(component: NetworkComponent): SerializableNetworkComponent {
    val parsedComponent = SerializableNetworkComponent()
    parsedComponent.clientPredicted = component.clientPredicted
    parsedComponent.ignorePrediction = component.ignorePrediction
    component.synchronizedProperties.forEach { (comp, properties) ->
        val componentName = inverseComponentNames[comp]!!
        parsedComponent.synchronizedProperties[componentName] = properties
    }
    return parsedComponent
}

/**
 * Changes the components to their XML serializable counterpart if they are not directly serializable.
 * @param components, a list of native components to convert to XML serializable components
 * @return a list of totally XML serializable components
 */
internal fun convertComponentsToSerializable(components: List<IComponent>) : ArrayList<IComponent> {
    val convertedComponents = ArrayList<IComponent>()
    components.forEach { component ->
        when (component) {
            is NetworkComponent -> convertedComponents.add(parseSerializableNetworkComponent((component)))
            else -> convertedComponents.add(component)
        }
    }
    return convertedComponents
}

/**
 * Changes XML serializable components to native components (Native: used by the engine's systems)
 * @param components, a list of totally XML serializable components (some of them are not supported by the systems)
 * @return a list of totally supported native components
 */
internal fun convertComponentsToNative(components: List<IComponent>) : ArrayList<IComponent> {
    val convertedComponents = ArrayList<IComponent>()
    components.forEach { component ->
        when (component) {
            is SerializableNetworkComponent -> convertedComponents.add(parseNetworkComponent(component))
            else -> convertedComponents.add(component)
        }
    }
    return convertedComponents
}


object SceneParser {
    /**
     * Loads a scene from an XML file, returning a ComponentMap containing all the components for each entity.
     * @param path the directory path to the xml file describing the scene.
     */
    fun loadScene(path: String): Scene {
        val xmlDecoder = XMLDecoder(BufferedInputStream(FileInputStream(path)))
        val components = xmlDecoder.readObject() as Scene

        // replace SerializableNetworkComponents as NetworkComponent
        components.forEach { (entity, comp) ->
            components[entity] = convertComponentsToNative(comp)
        }

        return components
    }

    /**
     * Saves a scene to a XML file, adding persistence.
     */
    fun saveScene(components: Scene, path: String) {
        // replace NetworkComponent as SerializableNetworkComponents
        val xmlEncoder = XMLEncoder(BufferedOutputStream(FileOutputStream(path)))

        val componentCopy = Scene()
        components.forEach { (entity, comp) ->
            components[entity] = convertComponentsToSerializable(comp)
        }

        xmlEncoder.writeObject(componentCopy)
        xmlEncoder.close()
    }
}