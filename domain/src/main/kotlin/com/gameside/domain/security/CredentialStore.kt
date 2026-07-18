package com.gameside.domain.security

interface CredentialStore {
    suspend fun put(identifier: String, credential: String)
    suspend fun get(identifier: String): String?
    suspend fun remove(identifier: String)
    suspend fun contains(identifier: String): Boolean
    suspend fun clearAll()
}
