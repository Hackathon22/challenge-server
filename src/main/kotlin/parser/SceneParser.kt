package parser

import components.*
import core.Entity
import core.IComponent
import core.Instance

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
        val components = XMLObjectReader.readObject<Scene>(path)

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
        val componentCopy = Scene()
        components.forEach { (entity, comp) ->
            components[entity] = convertComponentsToSerializable(comp)
        }

        XMLObjectWriter.writeObject(path, componentCopy)
    }
}


fun main() {
    val instance = Instance()

    val scene = HashMap<Int, ArrayList<IComponent>>()

    val cameraEntity = instance.createEntity()
    val cameraCameraComponent = CameraComponent()
    val cameraTransformComponent = TransformComponent()

    scene[cameraEntity] = arrayListOf(cameraCameraComponent, cameraTransformComponent)

    val simpleEntity = instance.createEntity()
    val simpleTransformComponent = TransformComponent()
    simpleTransformComponent.pos.x = -106.0f
    val simpleSpriteComponent = SpriteComponent()
    simpleSpriteComponent.sprite

    scene[simpleEntity] = arrayListOf(simpleTransformComponent, simpleSpriteComponent)

    val simpleEntity2 = instance.createEntity()
    val simpleTransformComponent2 = TransformComponent()
    simpleTransformComponent2.pos.x = 42.0f
    val simpleSpriteComponent2 = SpriteComponent("invalid")
    simpleSpriteComponent2.sprite = "error"

    scene[simpleEntity2] = arrayListOf(simpleTransformComponent2, simpleSpriteComponent2)

    val dynamicEntity = instance.createEntity()
    val dynamicDynamicComponent = DynamicComponent()
    dynamicDynamicComponent.speed.y = 200.0f
    val dynamicTransformComponent = TransformComponent()
    dynamicTransformComponent.pos.x = -32.0f
    val dynamicSpriteComponent = SpriteComponent()

    scene[dynamicEntity] = arrayListOf(dynamicTransformComponent, dynamicDynamicComponent, dynamicSpriteComponent)

    XMLObjectWriter.writeObject("src/main/resources/scenes/base_scene.xml", scene)
}