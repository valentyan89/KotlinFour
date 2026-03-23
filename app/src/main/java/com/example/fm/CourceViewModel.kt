package com.example.fm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class CourceViewModel : ViewModel() {
    private val _rate = MutableStateFlow<Double>(81.92)
    public val rate: StateFlow<Double> = _rate

    private val _trend = MutableStateFlow(0)
    public val trend: StateFlow<Int> = _trend

    init {
        viewModelScope.launch {
            while(true){
                delay(5000)
                emitation()
            }
        }
    }

    suspend fun emitation(){
        val oldRate = _rate.value
        val newRate = 81.92 + Random.nextDouble(-2.0, 2.0)
        _rate.emit(newRate)
        _trend.emit(
            value = when{
                oldRate > newRate -> -1
                oldRate < newRate -> 1
                else -> 0
            }
        )
    }

    fun buttonRefresher(){
        viewModelScope.launch {
            emitation()
        }
    }
}