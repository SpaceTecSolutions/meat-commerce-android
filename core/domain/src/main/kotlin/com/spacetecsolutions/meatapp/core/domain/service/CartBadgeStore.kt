package com.spacetecsolutions.meatapp.core.domain.service

import kotlinx.coroutines.flow.StateFlow

interface CartBadgeStore {
    val quantity: StateFlow<Int>
    fun update(quantity: Int)
}
