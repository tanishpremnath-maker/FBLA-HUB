package com.example.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt dependency injection.
 * Annotating with @HiltAndroidApp generates the application-level
 * dependency container that all other Hilt components attach to.
 */
@HiltAndroidApp
class FblaHubApp : Application()
