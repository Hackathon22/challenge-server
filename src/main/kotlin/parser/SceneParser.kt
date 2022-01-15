package parser

import NetworkComponent
import components.DynamicComponent
import components.TransformComponent
import core.Entity
import core.IComponent
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

typealias ComponentMap = HashMap<Entity, ArrayList<IComponent>>

object SceneParser {

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
     * Loads a scene from an XML file, returning a ComponentMap containing all the components for each entity.
     * @param path the directory path to the xml file describing the scene.
     */
    fun loadScene(path: String): ComponentMap {
        val xmlDecoder = XMLDecoder(BufferedInputStream(FileInputStream(path)))
        val components = xmlDecoder.readObject() as ComponentMap

        // replace SerializableNetworkComponents as NetworkComponent
        components.forEach { (entity, comp) ->
            for (idx in comp.indices) {
                if (comp[idx] is SerializableNetworkComponent) {
                    comp[idx] = parseNetworkComponent(comp[idx] as SerializableNetworkComponent)
                }
            }
        }

        return components
    }

    /**
     * Saves a scene to a XML file, adding persistence.
     */
    fun saveScene(components: ComponentMap, path: String) {
        // replace NetworkComponent as SerializableNetworkComponents
        val xmlEncoder = XMLEncoder(BufferedOutputStream(FileOutputStream(path)))

        val componentCopy = ComponentMap()
        components.forEach { (entity, comp) ->
            val componentListCopy = ArrayList<IComponent>()
            for (idx in comp.indices) {
                if (comp[idx] is NetworkComponent) {
                    componentListCopy.add(parseSerializableNetworkComponent(comp[idx] as NetworkComponent))
                } else {
                    componentListCopy.add(comp[idx])
                }
            }
            componentCopy[entity] = componentListCopy
        }

        println(componentCopy)
        xmlEncoder.writeObject(componentCopy)
        xmlEncoder.close()
    }
}