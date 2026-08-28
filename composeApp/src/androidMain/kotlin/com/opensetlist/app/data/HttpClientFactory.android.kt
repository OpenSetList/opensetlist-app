package com.opensetlist.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun buildHttpClient(): HttpClient = HttpClient(OkHttp)