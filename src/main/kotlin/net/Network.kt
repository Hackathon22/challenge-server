package net

import core.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


typealias ValuePairs = HashMap<String, Any>

/**
 * a map that gives for each component a list of (properties, value) changes
 */
typealias ComponentValues = HashMap<KClass<out IComponent>, ValuePairs>

/**
 * A map containing a ComponentValues map for each entity
 */
typealias ChangedProperties = HashMap<Int, ComponentValues>

/**
 * Component holding all the values to synchronize over network
 */
class NetworkComponent : IComponent {
    // map containing lists of component properties to synchronize for each Component
    val synchronizedProperties = HashMap<KClass<out IComponent>, List<String>>()
    val clientPredicted = false
    val ignorePrediction = false
}

/**
 * Interfaces that needs to implement actions useful from the network system such as pushing/polling snapshots
 */
interface NetworkSynchronizer {

    fun sendProperties(changes: ChangedProperties)
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
        private val _lastSentChanges = ChangedProperties()

        /**
         * A dictionary containing networked values that changed since the last tick and that will be sent over the network.
         */
        private val _committedChanges = ChangedProperties()

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
        fun commit(): ChangedProperties {
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
        private fun insertChange(entity: Entity, componentType: KClass<out IComponent>, valueName: String, value: Any) {
            if (_committedChanges[entity] == null) {
                _committedChanges[entity] = ComponentValues()
            }
            if (_committedChanges[entity]!![componentType] == null) {
                _committedChanges[entity]!![componentType] = ValuePairs()
            }
            _committedChanges[entity]!![componentType]!![valueName] = value
        }

        /**
         * Inserts a new (valueName, value) pair in the commit dictionary.
         */
        private fun insertSentChange(entity: Entity, componentType: KClass<out IComponent>, valueName: String, value: Any) {
            if (_lastSentChanges[entity] == null) {
                _lastSentChanges[entity] = ComponentValues()
            }
            if (_lastSentChanges[entity]!![componentType] == null) {
                _lastSentChanges[entity]!![componentType] = ValuePairs()
            }
            _lastSentChanges[entity]!![componentType]!![valueName] = value
        }

        /**
         * Checks wherever a change was already sent, and adds it to the committed changes if not.
         */
        fun addChange(entity: Entity, componentType: KClass<out IComponent>, valueName: String, value: Any) {
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

    private val _changeRecord = ChangeRecord()

    private var _synchronizer : NetworkSynchronizer? = null

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
    private fun saveProperties(instance: Instance, delta: Float) {
        for (entity in entities) {
            val networkComponent = instance.getComponent<NetworkComponent>(entity)
            // iterates over the components and values to gather
            for ((componentClass, valueList) in networkComponent.synchronizedProperties) {
                val component = instance.getComponentDynamic(entity, componentClass)
                // get the component's tracked values
                for (valueName in valueList) {
                    val value = readComponentProperties(component, valueName)
                    _changeRecord.addChange(entity, componentClass, valueName, value)
                }
            }
        }
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

    override fun updateLogic(instance: Instance, delta: Float) {
        // read all the tracked properties and saves them in the change record
        saveProperties(instance, delta)

        // commits the saved changes and get them
        val changedProperties = _changeRecord.commit()

        // send the changed properties over the network
        _synchronizer!!.sendProperties(changedProperties)
    }

}