package com.github.redditvanced.modals

import kotlinx.serialization.Serializable

@Serializable
data class AccountCredentialsModel(
    val username: String,
    val password: String
)
