package com.github.redditvanced.modals

import kotlinx.serialization.Serializable

@Serializable
data class AccountModal(
	val token: String,
	val gsfId: String,
)
