/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2013  Paul Ferraro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.sql.serial;

import java.sql.Array;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialArray;

/**
 * @author Paul Ferraro
 */
public class SerialArrayFactory implements SerialLocatorFactory<Array>
{
	@Override
	public Array createSerial(Array array) throws SQLException
	{
		return new SerialArray(array);
	}
}
