/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import net.sf.hajdbc.ForeignKeyConstraint;
import net.sf.hajdbc.QualifiedName;
import net.sf.hajdbc.SequenceProperties;
import net.sf.hajdbc.cache.ForeignKeyConstraintImpl;

import static org.mockito.Mockito.*;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
public class H2DialectTest extends StandardDialectTest
{
	public H2DialectTest()
	{
		super(DialectFactoryEnum.H2);
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getSequenceSupport()
	 */
	@Override
	public void getSequenceSupport()
	{
		Assert.assertSame(this.dialect, this.dialect.getSequenceSupport());
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getCreateForeignKeyConstraintSQL()
	 */
	@Override
	public void getCreateForeignKeyConstraintSQL() throws SQLException
	{
		QualifiedName table = mock(QualifiedName.class);
		QualifiedName foreignTable = mock(QualifiedName.class);
		
		when(table.getDDLName()).thenReturn("table");
		when(foreignTable.getDDLName()).thenReturn("foreign_table");
		
		ForeignKeyConstraint key = new ForeignKeyConstraintImpl("name", table);
		key.getColumnList().add("column1");
		key.getColumnList().add("column2");
		key.setForeignTable(foreignTable);
		key.getForeignColumnList().add("foreign_column1");
		key.getForeignColumnList().add("foreign_column2");
		key.setDeferrability(DatabaseMetaData.importedKeyInitiallyDeferred);
		key.setDeleteRule(DatabaseMetaData.importedKeyCascade);
		key.setUpdateRule(DatabaseMetaData.importedKeyRestrict);
		
		String result = this.dialect.getCreateForeignKeyConstraintSQL(key);
		
		Assert.assertEquals("ALTER TABLE table ADD CONSTRAINT name FOREIGN KEY (column1, column2) REFERENCES foreign_table (foreign_column1, foreign_column2) ON DELETE CASCADE ON UPDATE RESTRICT", result);
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getSequences()
	 */
	@Override
	public void getSequences() throws SQLException
	{
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		ResultSet resultSet = mock(ResultSet.class);
		
		when(metaData.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.executeQuery("SELECT SEQUENCE_SCHEMA, SEQUENCE_NAME, INCREMENT FROM INFORMATION_SCHEMA.SEQUENCES")).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
		when(resultSet.getString(1)).thenReturn("schema1").thenReturn("schema2");
		when(resultSet.getString(2)).thenReturn("sequence1").thenReturn("sequence2");
		when(resultSet.getInt(3)).thenReturn(1).thenReturn(2);
		
		Map<QualifiedName, Integer> results = this.dialect.getSequenceSupport().getSequences(metaData);
		
		verify(statement).close();
		
		Assert.assertEquals(results.size(), 2);
		
		Iterator<Map.Entry<QualifiedName, Integer>> entries = results.entrySet().iterator();
		Map.Entry<QualifiedName, Integer> entry = entries.next();

		Assert.assertEquals("schema1", entry.getKey().getSchema());
		Assert.assertEquals("sequence1", entry.getKey().getName());
		Assert.assertEquals(1, entry.getValue().intValue());
		
		entry = entries.next();
		
		Assert.assertEquals("schema2", entry.getKey().getSchema());
		Assert.assertEquals("sequence2", entry.getKey().getName());
		Assert.assertEquals(2, entry.getValue().intValue());
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getSimpleSQL()
	 */
	@Override
	public void getSimpleSQL() throws SQLException
	{
		Assert.assertEquals("CALL CURRENT_TIMESTAMP", this.dialect.getSimpleSQL());
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getNextSequenceValueSQL()
	 */
	@Override
	public void getNextSequenceValueSQL() throws SQLException
	{
		SequenceProperties sequence = mock(SequenceProperties.class);
		QualifiedName name = mock(QualifiedName.class);
		
		when(sequence.getName()).thenReturn(name);
		when(name.getDMLName()).thenReturn("sequence");
		
		String result = this.dialect.getSequenceSupport().getNextSequenceValueSQL(sequence);
		
		Assert.assertEquals("CALL NEXT VALUE FOR sequence", result);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#getDefaultSchemas()
	 */
	@Override
	public void getDefaultSchemas() throws SQLException
	{
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		List<String> result = this.dialect.getDefaultSchemas(metaData);
		
		Assert.assertEquals(1, result.size());
		Assert.assertEquals("PUBLIC", result.get(0));
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#evaluateCurrentDate()
	 */
	@Override
	public void evaluateCurrentDate()
	{
		java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
		
		Assert.assertEquals(String.format("SELECT DATE '%s' FROM test", date.toString()), this.dialect.evaluateCurrentDate("SELECT CURRENT_DATE FROM test", date));
		Assert.assertEquals(String.format("SELECT DATE '%s' FROM test", date.toString()), this.dialect.evaluateCurrentDate("SELECT CURRENT_DATE() FROM test", date));
		Assert.assertEquals(String.format("SELECT DATE '%s' FROM test", date.toString()), this.dialect.evaluateCurrentDate("SELECT CURRENT_DATE ( ) FROM test", date));
		Assert.assertEquals("SELECT CURDATE FROM test", this.dialect.evaluateCurrentDate("SELECT CURDATE FROM test", date));
		Assert.assertEquals(String.format("SELECT DATE '%s' FROM test", date.toString()), this.dialect.evaluateCurrentDate("SELECT CURDATE() FROM test", date));
		Assert.assertEquals(String.format("SELECT DATE '%s' FROM test", date.toString()), this.dialect.evaluateCurrentDate("SELECT CURDATE ( ) FROM test", date));
		Assert.assertEquals("SELECT CCURRENT_DATE FROM test", this.dialect.evaluateCurrentDate("SELECT CCURRENT_DATE FROM test", date));
		Assert.assertEquals("SELECT CURRENT_DATES FROM test", this.dialect.evaluateCurrentDate("SELECT CURRENT_DATES FROM test", date));
		Assert.assertEquals("SELECT CURRENT_TIME FROM test", this.dialect.evaluateCurrentDate("SELECT CURRENT_TIME FROM test", date));
		Assert.assertEquals("SELECT CURRENT_TIMESTAMP FROM test", this.dialect.evaluateCurrentDate("SELECT CURRENT_TIMESTAMP FROM test", date));
		Assert.assertEquals("SELECT 1 FROM test", this.dialect.evaluateCurrentDate("SELECT 1 FROM test", date));
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#evaluateCurrentTime()
	 */
	@Override
	public void evaluateCurrentTime()
	{
		java.sql.Time time = new java.sql.Time(System.currentTimeMillis());
		
		Assert.assertEquals(String.format("SELECT TIME '%s' FROM test", time.toString()), this.dialect.evaluateCurrentTime("SELECT CURRENT_TIME FROM test", time));
		Assert.assertEquals(String.format("SELECT TIME '%s' FROM test", time.toString()), this.dialect.evaluateCurrentTime("SELECT CURRENT_TIME() FROM test", time));
		Assert.assertEquals(String.format("SELECT TIME '%s' FROM test", time.toString()), this.dialect.evaluateCurrentTime("SELECT CURRENT_TIME ( ) FROM test", time));
		Assert.assertEquals("SELECT CURTIME FROM test", this.dialect.evaluateCurrentTime("SELECT CURTIME FROM test", time));
		Assert.assertEquals(String.format("SELECT TIME '%s' FROM test", time.toString()), this.dialect.evaluateCurrentTime("SELECT CURTIME() FROM test", time));
		Assert.assertEquals(String.format("SELECT TIME '%s' FROM test", time.toString()), this.dialect.evaluateCurrentTime("SELECT CURTIME ( ) FROM test", time));
		Assert.assertEquals("SELECT LOCALTIME FROM test", this.dialect.evaluateCurrentTime("SELECT LOCALTIME FROM test", time));
		Assert.assertEquals("SELECT LOCALTIME(2) FROM test", this.dialect.evaluateCurrentTime("SELECT LOCALTIME(2) FROM test", time));
		Assert.assertEquals("SELECT LOCALTIME ( 2 ) FROM test", this.dialect.evaluateCurrentTime("SELECT LOCALTIME ( 2 ) FROM test", time));
		Assert.assertEquals("SELECT CCURRENT_TIME FROM test", this.dialect.evaluateCurrentTime("SELECT CCURRENT_TIME FROM test", time));
		Assert.assertEquals("SELECT LLOCALTIME FROM test", this.dialect.evaluateCurrentTime("SELECT LLOCALTIME FROM test", time));
		Assert.assertEquals("SELECT CURRENT_DATE FROM test", this.dialect.evaluateCurrentTime("SELECT CURRENT_DATE FROM test", time));
		Assert.assertEquals("SELECT CURRENT_TIMESTAMP FROM test", this.dialect.evaluateCurrentTime("SELECT CURRENT_TIMESTAMP FROM test", time));
		Assert.assertEquals("SELECT LOCALTIMESTAMP FROM test", this.dialect.evaluateCurrentTime("SELECT LOCALTIMESTAMP FROM test", time));
		Assert.assertEquals("SELECT 1 FROM test", this.dialect.evaluateCurrentTime("SELECT 1 FROM test", time));
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.dialect.StandardDialectTest#evaluateCurrentTimestamp()
	 */
	@Override
	public void evaluateCurrentTimestamp()
	{
		java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		
		Assert.assertEquals(String.format("SELECT TIMESTAMP '%s' FROM test", timestamp.toString()), this.dialect.evaluateCurrentTimestamp("SELECT CURRENT_TIMESTAMP FROM test", timestamp));
		Assert.assertEquals(String.format("SELECT TIMESTAMP '%s' FROM test", timestamp.toString()), this.dialect.evaluateCurrentTimestamp("SELECT CURRENT_TIMESTAMP() FROM test", timestamp));
		Assert.assertEquals(String.format("SELECT TIMESTAMP '%s' FROM test", timestamp.toString()), this.dialect.evaluateCurrentTimestamp("SELECT CURRENT_TIMESTAMP ( ) FROM test", timestamp));
		Assert.assertEquals("SELECT NOW FROM test", this.dialect.evaluateCurrentTimestamp("SELECT NOW FROM test", timestamp));
		Assert.assertEquals(String.format("SELECT TIMESTAMP '%s' FROM test", timestamp.toString()), this.dialect.evaluateCurrentTimestamp("SELECT NOW() FROM test", timestamp));
		Assert.assertEquals(String.format("SELECT TIMESTAMP '%s' FROM test", timestamp.toString()), this.dialect.evaluateCurrentTimestamp("SELECT NOW ( ) FROM test", timestamp));
		Assert.assertEquals("SELECT LOCALTIMESTAMP FROM test", this.dialect.evaluateCurrentTimestamp("SELECT LOCALTIMESTAMP FROM test", timestamp));
		Assert.assertEquals("SELECT LOCALTIMESTAMP(2) FROM test", this.dialect.evaluateCurrentTimestamp("SELECT LOCALTIMESTAMP(2) FROM test", timestamp));
		Assert.assertEquals("SELECT LOCALTIMESTAMP ( 2 ) FROM test", this.dialect.evaluateCurrentTimestamp("SELECT LOCALTIMESTAMP ( 2 ) FROM test", timestamp));
		Assert.assertEquals("SELECT CCURRENT_TIMESTAMP FROM test", this.dialect.evaluateCurrentTimestamp("SELECT CCURRENT_TIMESTAMP FROM test", timestamp));
		Assert.assertEquals("SELECT LLOCALTIMESTAMP FROM test", this.dialect.evaluateCurrentTimestamp("SELECT LLOCALTIMESTAMP FROM test", timestamp));
		Assert.assertEquals("SELECT CURRENT_DATE FROM test", this.dialect.evaluateCurrentTimestamp("SELECT CURRENT_DATE FROM test", timestamp));
		Assert.assertEquals("SELECT CURRENT_TIME FROM test", this.dialect.evaluateCurrentTimestamp("SELECT CURRENT_TIME FROM test", timestamp));
		Assert.assertEquals("SELECT LOCALTIME FROM test", this.dialect.evaluateCurrentTimestamp("SELECT LOCALTIME FROM test", timestamp));
		Assert.assertEquals("SELECT 1 FROM test", this.dialect.evaluateCurrentTimestamp("SELECT 1 FROM test", timestamp));
	}
}
