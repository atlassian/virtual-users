package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.data.generators.NameGenerator
import java.util.concurrent.atomic.AtomicInteger

internal class UserNameGenerator {
    private val nameGenerator = NameGenerator()
    private val givenNamesCnt = AtomicInteger(1)
    
    private val collisionsSinceReset = AtomicInteger()
    private val successesSinceLastCollision = AtomicInteger()
    
    fun pickRandomUnique(givenNamesCnt: Int): String {
        return nameGenerator.pickRandomUnique(givenNamesCnt)
    }

    fun getNumberOfGivenNamesToGenerate(): Int {
        return givenNamesCnt.toInt()
    }

    fun onCollision(givenNamesUsedOnCollision: Int) {
        successesSinceLastCollision.set(0)
        val currentGivenNamesCnt = givenNamesCnt.get()

        //we increase the count of given names after second collision since reset
        if (currentGivenNamesCnt<=givenNamesUsedOnCollision && collisionsSinceReset.incrementAndGet()>=2) {
            givenNamesCnt.compareAndSet(currentGivenNamesCnt, givenNamesUsedOnCollision + 1)
        }
    }

    fun onSuccess() {
        if (successesSinceLastCollision.incrementAndGet() % 100 == 0) {
            collisionsSinceReset.set(0)
        }
    }
}
