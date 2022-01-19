package systems

import components.NetworkComponent
import core.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


typealias ValuePairs = HashMap<String, Any>

/**
 * a map that gives for each component a list of (properties, value) changes
 */
typealias ComponentProperties = HashMap<KClass<out IComponent>, ValuePairs>

/**
 * A map containing a ComponentValues map for each entity
 */
typealias NetworkedProperties = HashMap<UUID, ComponentProperties>

/**
 * A map containing the entities from their network ID
 */
typealias NetworkMap = HashMap<UUID, Entity>

/**
 * A list containing for each networked entity a list of snapshots
 */
typealias FullSnapshot = ArrayList<ArrayList<IComponent>>

/**
 * Interfaces that needs to implement actions useful from the network system such as pushing/polling snapshots
 */
interface NetworkSynchronizer {

    fun sendProperties(changes: NetworkedProperties, addedEntities: FullSnapshot, removedEntities : List<UUID>)

    fun getEntityNetworkID(entity: Entity): UUID?

    fun setEntityNetworkID(entity: Entity, networkID : UUID?)
}

/**
 * The server network system will poll every entity that is NetworkSynchronized and pull
 */
class ServerNetworkSystem : System() {

    /**
     * Internal class that will take care of saving all the changes.
     */
    class ChangeRecord {

        /**
         * A dictionary containing all the values that where previously sent.
         * This dictionary is checked to see if a networked value has already been sent or not over the network.
         * This is useful to avoid sending values that didn't change since the last tick.
         */
        private val _lastSentChanges = NetworkedProperties()

        /**
         * A dictionary containing networked values that changed since the last tick and that will be sent over the network.
         */
        private val _committedChanges = NetworkedProperties()

        /**
         * Commit flag. Is set to true when all the changes have been saved and are ready to be sent over network.
         */
        private var _committed = false

        /**
         * Saves the commited changes to the last sent changes dictionary. THose registered values will not be sent over
         * the network in the next tick if they don't change.
         */
        private fun saveLastSentChanges() {
            for ((entity, componentType)  in _committedChanges) {
                val entityMap = _committedChanges[entity]!!
                for ((componentType, values) in entityMap) {
                    val valueMap = entityMap[componentType]!!
                    for ((valueName, value) in valueMap) {
                        insertSentChange(entity, componentType, valueName, value)
                    }
                }
            }
        }

        /**
         * Commits the changes and returns the dictionary containing them.
         */
        fun commit(): NetworkedProperties {
            _committed = true
            return _committedChanges
        }

        /**
         * If the commit hasn't been cleaned yet, clears the commit dictionnary.
         */
        private fun cleanCommit() {
            if (_committed) {
                saveLastSentChanges()
                _committedChanges.clear()
                _committed = false
            }
        }

        /**
         * Inserts a new (valueName, value) pair in the commit dictionary.
         */
        private fun insertChange(entity: UUID, componentType: KClass<out IComponent>, valueName: String, value: Any) {
            if (_committedChanges[entity] == null) {
                _committedChanges[entity] = ComponentProperties()
            }
            if (_committedChanges[entity]!![componentType] == null) {
                _committedChanges[entity]!![componentType] = ValuePairs()
            }
            _committedChanges[entity]!![componentType]!![valueName] = value
        }

        /**
         * Inserts a new (valueName, value) pair in the commit dictionary.
         */
        private fun insertSentChange(entity: UUID, componentType: KClass<out IComponent>, valueName: String, value: Any) {
            if (_lastSentChanges[entity] == null) {
                _lastSentChanges[entity] = ComponentProperties()
            }
            if (_lastSentChanges[entity]!![componentType] == null) {
                _lastSentChanges[entity]!![componentType] = ValuePairs()
            }
            _lastSentChanges[entity]!![componentType]!![valueName] = value
        }

        /**
         * Checks wherever a change was already sent, and adds it to the committed changes if not.
         */
        fun addChange(entity: UUID, componentType: KClass<out IComponent>, valueName: String, value: Any) {
            cleanCommit()
            // checks if there is such value was already sent in a previous tick
            val lastSentChange = _lastSentChanges[entity]?.get(componentType)?.get(valueName)
            if (lastSentChange != null) {
                // the value was sent, checks if it changed since then
                if (!lastSentChange.equals(value)) {
                    insertChange(entity, componentType, valueName, value)
                }
            }
            else {
                // the value was never sent, commits it in any case
                insertChange(entity, componentType, valueName, value)
            }
        }
    }

    /**
     * A wrapper that takes care of retrieving only the values that changed.
     */
    private val _changeRecord = ChangeRecord()

    /**
     * A dictionary mapping the entities from their network ID.
     */
    private val _networkMap = NetworkMap()

    /**
     * Interface object that allows to send data to the network and retrieve entity IDs without needing
     * to directly access to the server routines or the game instance.
     */
    private var _synchronizer : NetworkSynchronizer? = null

    /**
     * Array containing all the entities added since the last sent delta snapshot. The contained entities will be
     * sent in the next delta snapshot and then removed from this list.
     */
    private var _addedEntities = ArrayList<Entity>()

    /**
     * Same as added entities, but for removed entities. The network ID is used instead as it simply needs to be sent
     * over network, unlike added Entities where components values need to be retrieved first by this system.
     */
    private var _removedEntities = ArrayList<UUID>()

    /**
     * Dynamically reads a property from a component and the property name
     */
    @Suppress("UNCHECKED_CAST")
    private fun readComponentProperties(component: IComponent, propertyName: String): Any {
        val property = component::class.members.find { it.name == propertyName } as KProperty1<Any, *>
        return property.get(component)!!
    }

    /**
     * Reads all the properties tracked by the NetworkComponent.
     */
    private fun saveProperties(properties: NetworkedProperties) {
        properties.forEach { (networkID, components) ->
            components.forEach { (componentType, propertyList) ->
                propertyList.forEach { (propertyName, propertyValue) ->
                    _changeRecord.addChange(networkID, componentType, propertyName, propertyValue)
                }
            }
        }
    }

    private fun loadNetworkedProperties(instance: Instance) : NetworkedProperties {
        val properties = NetworkedProperties()
        entities.forEach { entity ->
            val networkComponent = instance.getComponent<NetworkComponent>(entity)
            properties[networkComponent.networkID!!] = ComponentProperties()
            // iterates over the components and values
            networkComponent.synchronizedProperties.forEach { (componentType, propertyList) ->
                val component = instance.getComponentDynamic(entity, componentType)
                val componentProperties = ValuePairs()
                // get the component's tracked values
                propertyList.forEach { propertyName ->
                    val value = readComponentProperties(component, propertyName)
                    componentProperties[propertyName] = value
                }
                properties[networkComponent.networkID!!]!![componentType] = componentProperties
            }
        }
        return properties
    }

    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.size != 1) {
            return false
        }
        if (arg[0] is NetworkSynchronizer) {
            _synchronizer = arg[0] as NetworkSynchronizer
            return true;
        }
        return false;
    }

    /**
     * Returns a {@link FullSnapshot} object containing all the components to all the networked entities (even the
     * components that are generally untracked by network.
     *
     * @param instance, the game instance containing the components
     * @return a FullSnapshot
     */
    fun getFullSnapshot(instance: Instance) : FullSnapshot {
        val snapshot = FullSnapshot()
        for (entity in entities) {
            snapshot.add(instance.getAllComponents(entity))
        }
        return snapshot
    }

    /**
     * Sends a delta snapshot update to all the clients through the {@link NetworkSynchronizer} interface.
     * The delta snapshot updates contains:
     *      - A partial FullSnapshot containing all component data of added entities,
     *      - A ChangedProperties object containing all the networked properties that changed since the last update.
     *      - A list of UUID of all the networked entities that where removed.
     *
     * @param instance the game instance containing all the components
     * @param delta the time elapsed since the last delta snapshot
     */
    override fun updateLogic(instance: Instance, delta: Float) {
        // read all the components from the added entities and sends them entirely over the network
        val addedEntitiesComponents = FullSnapshot()
        for (entity in _addedEntities) {
            addedEntitiesComponents.add(instance.getAllComponents(entity))
        }
        _addedEntities.clear()

        // read all the tracked properties and saves them in the change record
        val loadedProperties = loadNetworkedProperties(instance)
        saveProperties(loadedProperties)

        // commits the saved changes and get them
        val changedProperties = _changeRecord.commit()

        // send the changed properties over the network
        _synchronizer!!.sendProperties(changedProperties, addedEntitiesComponents, _removedEntities)
        _removedEntities.clear()
    }

    override fun onEntityAdded(entity: Entity) {
        _addedEntities.add(entity)
        val networkID = UUID.randomUUID()
        _synchronizer!!.setEntityNetworkID(entity, networkID)
        _networkMap[networkID] = entity
    }

    override fun onEntityRemoved(entity: Entity) {
        val networkID = _synchronizer!!.getEntityNetworkID(entity)
        _removedEntities.add(networkID!!)
        _synchronizer!!.setEntityNetworkID(entity, null)
        _networkMap.remove(networkID)
    }
}
