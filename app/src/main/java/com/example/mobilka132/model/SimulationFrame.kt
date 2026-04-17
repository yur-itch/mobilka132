package com.example.mobilka132.model

import com.example.mobilka132.data.ant.Ant
import com.example.mobilka132.data.ant.CoworkingSpace

data class SimulationFrame(
    val ants: List<Ant> = emptyList(),
    val spaces: List<CoworkingSpace> = emptyList(),
    val info: String = ""
)
