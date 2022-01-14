package net

import NetworkComponent
import components.DynamicComponent
import components.TransformComponent
import core.*
import org.junit.jupiter.api.Test
import systems.MovementSystem

internal class TestNetworkSystem {

    @Test
    fun testSimpleEntity() {
        val instance = Instance()

        instance.registerComponent<TransformComponent>()
        instance.registerComponent<DynamicComponent>()
        instance.registerComponent<NetworkComponent>()

        var savedChanges : ChangedProperties? = null

        // initializes the moving system
        val movementSystem = instance.registerSystem<MovementSystem>()
        movementSystem.initialize()

        // registers the moving system signature
        val movementSignature = Signature()
        movementSignature.set(instance.getComponentType<TransformComponent>())
        movementSignature.set(instance.getComponentType<DynamicComponent>())
        instance.setSystemSignature<MovementSystem>(movementSignature)

        // creates a network system and register its signature
        val networkSystem = instance.registerSystem<ServerNetworkSystem>()

        val networkSignature = Signature()
        networkSignature.set(instance.getComponentType<NetworkComponent>())
        instance.setSystemSignature<ServerNetworkSystem>(networkSignature)

        // implements a network synchronizer
        val networkSynchronizer = object : NetworkSynchronizer {
            override fun sendProperties(changes: ChangedProperties) {
                savedChanges = changes
            }
        }

        // important or the network doesn't work
        networkSystem.initialize(networkSynchronizer)

        val entity = instance.createEntity()

        // create transform component
        val entityTransformComponent = TransformComponent()
        instance.addComponent(entity, entityTransformComponent)

        // create dynamic component
        val entityDynamicComponent = DynamicComponent()
        entityDynamicComponent.speed.x = 5.0f
        instance.addComponent(entity, entityDynamicComponent)

        // network component, will track the values
        val entityNetworkComponent = NetworkComponent()
        entityNetworkComponent.synchronizedProperties[TransformComponent::class] = arrayListOf("pos")
        entityNetworkComponent.synchronizedProperties[DynamicComponent::class] = arrayListOf("speed")
        instance.addComponent(entity, entityNetworkComponent)

        // simulate the network system
        movementSystem.update(instance, 1.0f)
        networkSystem.update(instance, 1.0f)

        assert(savedChanges != null)
        assert(savedChanges!![entity]!![TransformComponent::class]!!["pos"] == entityTransformComponent.pos)
        assert(savedChanges!![entity]!![DynamicComponent::class]!!["speed"] == entityDynamicComponent.speed)

        println("Position changed to ${entityTransformComponent.pos}")

        // simulates the network system again, only this time the speed has already been sent and didn't change since
        // the last time, so we expect not to be included in savedChanges
        savedChanges = null


        movementSystem.update(instance, 1.0f)
        networkSystem.update(instance, 1.0f)

        assert(savedChanges != null)
        assert(savedChanges!![entity]!![TransformComponent::class]!!["pos"] == entityTransformComponent.pos)
        assert(savedChanges!![entity]!![DynamicComponent::class] == null)
    }
}