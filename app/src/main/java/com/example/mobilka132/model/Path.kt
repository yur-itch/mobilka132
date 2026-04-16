package com.example.mobilka132.model

import androidx.compose.ui.geometry.Offset

data class Path (
    val steps : List<Offset>,
    val distance : Float,

)