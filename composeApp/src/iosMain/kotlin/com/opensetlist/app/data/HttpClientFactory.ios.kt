package com.opensetlist.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun buildHttpClient(): HttpClient = HttpClient(Darwin)