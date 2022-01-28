package com.github.redditvanced

import com.github.redditvanced.modals.AccountModal
import com.github.theapache64.gpa.api.Play
import com.github.theapache64.gpa.model.Account
import java.time.Instant
import java.time.temporal.ChronoUnit

object GAccountHandler {
	private val email = System.getenv("GOOGLE_EMAIL")
	private val password = System.getenv("GOOGLE_PASSWORD")

	private var account: Account? = null
	private var loginTime = Instant.now()

	private suspend fun getAccount(): Account {
		if (account == null || loginTime.plus(12, ChronoUnit.HOURS) < Instant.now()) {
			account = Play.login(email, password)
			loginTime = Instant.now()
		}
		return account!!
	}

	suspend fun getAccountModal(): AccountModal =
		getAccount().run { AccountModal(token, gsfId) }
}
