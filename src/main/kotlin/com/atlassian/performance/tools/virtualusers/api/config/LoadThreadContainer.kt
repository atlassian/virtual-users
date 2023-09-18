package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import net.jcip.annotations.ThreadSafe
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * TODO return interfaces/abstracts only
 * TODO ensure all types are in API
 */
@ThreadSafe
class LoadThreadContainer(
    private val processContainer: LoadProcessContainer,
    private val index: Supplier<Int>,
    private val id: Supplier<String>,
    private val random: Supplier<Random>,
    private val actionMeter: Supplier<ActionMeter>,
    private val taskMeter: Supplier<ActionMeter>,
    private val threadResult: Supplier<VirtualUserResult>,
    private val singleThreadLoad: Supplier<VirtualUserLoad>,
    private val addClosable: Consumer<AutoCloseable>,
    private val onClose: Runnable
) : AutoCloseable {

    fun index() = index.get()
    fun id() = id.get()

    fun threadResult() = threadResult.get()

    fun random() = random.get()

    fun actionMeter() = actionMeter.get()

    fun taskMeter() = taskMeter.get()

    fun singleThreadLoad() = singleThreadLoad.get()

    fun loadProcessContainer() = processContainer

    fun addCloseable(closeable: AutoCloseable) {
        addClosable.accept(closeable)
    }

    override fun close() = onClose.run()

    internal class Builder(
        private val defaultContainer: LoadThreadContainerDefaults
    ) {

        private var processContainer: LoadProcessContainer = defaultContainer.loadProcessContainer()
        private var index: Supplier<Int> = Supplier { defaultContainer.index }
        private var id: Supplier<String> = Supplier(defaultContainer::id)
        private var random: Supplier<Random> = Supplier(defaultContainer::random)
        private var actionMeter: Supplier<ActionMeter> = Supplier(defaultContainer::actionMeter)
        private var taskMeter: Supplier<ActionMeter> = Supplier { defaultContainer.taskMeter() }
        private var threadResult: Supplier<VirtualUserResult> = Supplier(defaultContainer::threadResult)
        private var singleThreadLoad: Supplier<VirtualUserLoad> = Supplier(defaultContainer::singleThreadLoad)
        private var addClosable: Consumer<AutoCloseable> = Consumer(defaultContainer::addCloseable)
        private var onClose: Runnable = Runnable(defaultContainer::close)

        fun id(id: Supplier<String>) = apply { this.id = id }
        fun random(random: Supplier<Random>) = apply { this.random = random }
        fun actionMeter(actionMeter: Supplier<ActionMeter>) = apply { this.actionMeter = actionMeter }
        fun taskMeter(taskMeter: Supplier<ActionMeter>) = apply { this.taskMeter = taskMeter }
        fun threadResult(threadResult: Supplier<VirtualUserResult>) = apply { this.threadResult = threadResult }
        fun singleThreadLoad(singleThreadLoad: Supplier<VirtualUserLoad>) =
            apply { this.singleThreadLoad = singleThreadLoad }

        fun addClosable(addClosable: Consumer<AutoCloseable>) = apply { this.addClosable = addClosable }
        fun onClose(onClose: Runnable) = apply { this.onClose = onClose }

        fun build() = LoadThreadContainer(
            processContainer = processContainer,
            index = index,
            id = id,
            random = random,
            actionMeter = actionMeter,
            taskMeter = taskMeter,
            threadResult = threadResult,
            singleThreadLoad = singleThreadLoad,
            addClosable = addClosable,
            onClose = onClose
        )
    }
}

