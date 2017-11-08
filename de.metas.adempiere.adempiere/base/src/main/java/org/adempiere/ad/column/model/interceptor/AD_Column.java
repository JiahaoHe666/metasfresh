package org.adempiere.ad.column.model.interceptor;

import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.compiere.model.AccessSqlParser;
import org.compiere.model.I_AD_Column;
import org.compiere.model.ModelValidator;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Interceptor(I_AD_Column.class)
public class AD_Column
{
	public static final transient AD_Column instance = new AD_Column();
	

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW, ModelValidator.TYPE_BEFORE_CHANGE }, ifColumnsChanged = I_AD_Column.COLUMNNAME_ColumnSQL)
	public void lowerCaseWhereClause(final I_AD_Column column)
	{
		final String columnSQL = column.getColumnSQL();
		
		if(columnSQL.isEmpty())
		{
			// nothing to do
			return;
		}
		
		final AccessSqlParser accessSqlParserInstance = new AccessSqlParser();
		final String adaptedWhereClause = accessSqlParserInstance.rewriteWhereClauseWithLowercaseKeyWords(columnSQL);
		
		column.setColumnSQL(adaptedWhereClause);
		
	}

}
