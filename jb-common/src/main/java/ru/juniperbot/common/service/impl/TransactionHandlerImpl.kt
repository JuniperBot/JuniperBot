/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.service.impl

import org.hibernate.StaleStateException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.juniperbot.common.service.TransactionHandler

@Service
open class TransactionHandlerImpl : TransactionHandler {

    @Transactional(propagation = Propagation.REQUIRED)
    override fun runInTransaction(action: Runnable) {
        return action.run();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun runInNewTransaction(action: Runnable) {
        return action.run();
    }

    @Retryable(StaleStateException::class, maxAttempts = 5, backoff = Backoff(delay = 500))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun runWithLockRetry(action: Runnable) {
        return action.run();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    override fun <T> runInTransaction(action: () -> T): T {
        return action.invoke();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun <T> runInNewTransaction(action: () -> T): T {
        return action.invoke();
    }

    @Retryable(StaleStateException::class, maxAttempts = 5, backoff = Backoff(delay = 500))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun <T> runWithLockRetry(action: () -> T): T {
        return action.invoke();
    }
}