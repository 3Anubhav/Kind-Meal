package com.example.registration_form

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationBarItems(val icon: ImageVector) {
    Person(Icons.Default.Person),
    Call(Icons.Default.Call),
    Settings(Icons.Default.Settings)
}
