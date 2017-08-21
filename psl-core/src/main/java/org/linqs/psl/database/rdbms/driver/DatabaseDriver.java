/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2017 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.database.rdbms.driver;

import org.linqs.psl.model.term.ConstantType;

import com.healthmarketscience.sqlbuilder.CreateTableQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;

public interface DatabaseDriver {

	/**
	 * Returns a connection to the database. Database drivers are expected to
	 * fully connect at instantiation (i.e. in the constructor).
	 * @return the connection to the database, as specified in the DatabaseDriver constructor
	 */
	public Connection getConnection();

	/**
	 * Returns whether the underline database supports external java functions.
	 * Distinguish from H2 Java External Function Support, which is very special.
	 * @return true if support H2 in memory java method, false if not support
	 */
	public boolean isSupportExternalFunction();

	/**
	 * Get the type name for each argument type.
	 */
	public String getTypeName(ConstantType type);

	/**
	 * Get the SQL definition for a primary, surrogate (auto-increment) key
	 * for use in a CREATE TABLE statement.
	 */
	public String getSurrogateKeyColumnDefinition(String columnName);

	/**
	 * Get the type name for a double type.
	 */
	public String getDoubleTypeName();

	/**
	 * Get a PreparedStatement for an upsert (merge) on the specified table and columns.
	 * An "upsert" updates existing records and inserts where there is no record.
	 * Most RDBMSs support some for of upsert, but the syntax is inconsistent.
	 * The parameters for the statement should the the specified columns in order.
	 * Some databases (like H2) require knowing the key columns we need to use.
	 */
	public PreparedStatement getUpsert(Connection connection, String tableName,
			String[] columns, String[] keyColumns);

	/**
	 * Gives the driver a chance to perform any final
	 * manipulations to the CREATE TABLE statement.
	 */
	public String finalizeCreateTable(CreateTableQuery createTable);
}
