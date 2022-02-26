package com.github.redditvanced.database

import dev.kord.common.entity.Snowflake
import org.ktorm.schema.*
import java.sql.*

fun BaseTable<*>.snowflake(name: String): Column<Snowflake> =
	registerColumn(name, SnowflakeSqlType)

object SnowflakeSqlType : SqlType<Snowflake>(Types.BIGINT, "snowflake") {
	override fun doGetResult(rs: ResultSet, index: Int): Snowflake =
		Snowflake(rs.getLong(index))

	override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Snowflake) =
		ps.setLong(index, parameter.value.toLong())
}
