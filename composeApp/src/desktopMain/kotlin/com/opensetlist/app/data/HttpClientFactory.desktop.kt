package com.opensetlist.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun buildHttpClient(): HttpClient = HttpClient(CIO)