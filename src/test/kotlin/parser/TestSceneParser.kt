package parser

import NetworkComponent
import components.TransformComponent
import core.Instance
import org.junit.jupiter.api.Test

class TestSceneParser {
    @Test
    fun testSaveScene() {
        val savePath = "src/test/resources/output/scenes/testScene1.xml"

        val instance = Instance()
        instance.registerComponent<TransformComponent>()
        instance.registerComponent<NetworkComponent>()

        // networked resource
        val entity1 = instance.createEntity()

        // non networked resource
        val entity2 = instance.createEntity()

        val transformComponent = TransformComponent()
        transformComponent.pos.x = 1
        transformComponent.pos.y = 2
        transformComponent.pos.z = 0
        instance.addComponent(entity1, transformComponent)

        val networkComponent = NetworkComponent()
        networkComponent.synchronizedProperties[TransformComponent::class] = arrayListOf("pos", "rot", "scale")
        instance.addComponent(entity1, networkComponent)

        val transformComponent2 = TransformComponent()
        transformComponent2.pos.x = -1
        transformComponent2.pos.y = 0
        transformComponent2.pos.z = 0
        instance.addComponent(entity2, transformComponent2)

        // get a component map ready for initialization
        val componentMap = instance.createComponentMap()
        SceneParser.saveScene(componentMap, savePath)
    }

    @Test
    fun testLoadScene() {
        val scenePath = "src/test/resources/scenes/testScene1.xml"

        val componentMap = SceneParser.loadScene(scenePath)
        val instance = Instance()
        instance.registerComponent<TransformComponent>()
        instance.registerComponent<NetworkComponent>()
        instance.loadComponentMap(componentMap)

        val networkComponent = instance.getComponent<NetworkComponent>(0)
        assert(networkComponent.synchronizedProperties[TransformComponent::class]!!.size == 3)

        val transformComponent = instance.getComponent<TransformComponent>(0)
        assert(transformComponent.pos.x == 1)
        assert(transformComponent.pos.y == 2)
        assert(transformComponent.pos.z == 0)

        val transformComponent2 = instance.getComponent<TransformComponent>(1)
        println(transformComponent2.pos)
        assert(transformComponent2.pos.x == -1)
        assert(transformComponent2.pos.y == 0)
        assert(transformComponent2.pos.z == 0)

    }

}