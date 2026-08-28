package com.opensetlist.app.data

import io.ktor.client.HttpClient

/**
 * Cria o cliente HTTP da plataforma corrente (usado na busca de cifras online).
 */
expect fun buildHttpClient(): HttpClient