package com.example.opencut

import android.app.Application
import com.example.opencut.data.OpenCutDatabase
import com.example.opencut.data.ProjectRepository

class OpenCutApp : Application() {
    val database: OpenCutDatabase by lazy {
        OpenCutDatabase.getInstance(this)
    }

    val repository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao())
    }
}
