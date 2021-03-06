/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

package net.ddp2p.common.streaming;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.HashSet;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.hds.Table;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class UpdateNewsTable {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static byte[] getFieldData(Object object) {
		return UpdateMessages.getFieldData(object);
	}
	public static Table buildNewsTable(String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs, int limitNewsLow, int limitNewsMax) throws P2PDDSQLException{
		return buildNewsTable1(last_sync_date, _maxDate, justDate, orgs, limitNewsLow, limitNewsMax);
	}
	public static void integrateNewsTable(Table tab) throws P2PDDSQLException{
		integrateNewsTable1(tab);
	}
	private static Table buildNewsTable1(String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs, int limitNewsLow, int limitNewsMax) throws P2PDDSQLException {
		if(justDate) return null;
		Table recentNews=new Table();
		recentNews.name = net.ddp2p.common.table.news.G_TNAME;
		recentNews.fields=DD.newsFields;
		recentNews.fieldTypes=DD.newsFieldsTypes;
		String queryMaxDate = 
			"SELECT n."+net.ddp2p.common.table.news.news_ID+", "+net.ddp2p.common.table.constituent.global_constituent_ID+
			", n."+net.ddp2p.common.table.news.creation_date+", "+net.ddp2p.common.table.news.news+", n."+net.ddp2p.common.table.news.type+", n."+net.ddp2p.common.table.news.signature +
			" FROM "+net.ddp2p.common.table.news.TNAME+" AS n " +
					" JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON (n."+net.ddp2p.common.table.news.constituent_ID+" == c."+net.ddp2p.common.table.constituent.constituent_ID+") " +
							" WHERE n." +net.ddp2p.common.table.news.organization_ID+" IS NULL AND "+
							" n."+net.ddp2p.common.table.news.broadcasted+"<>'0' AND "+
							" ( n."+net.ddp2p.common.table.news.arrival_date+" > ? ) AND ( n."+net.ddp2p.common.table.news.arrival_date+" <= ? ) LIMIT "+limitNewsLow;
		String queryNoMaxDate =
			"SELECT n."+net.ddp2p.common.table.news.news_ID+", "+net.ddp2p.common.table.constituent.global_constituent_ID+
			", n."+net.ddp2p.common.table.news.creation_date+", "+net.ddp2p.common.table.news.news+", n."+net.ddp2p.common.table.news.type+", n."+net.ddp2p.common.table.news.signature +
			" FROM "+net.ddp2p.common.table.news.TNAME+" AS n " +
					" JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON (n."+net.ddp2p.common.table.news.constituent_ID+" == c."+net.ddp2p.common.table.constituent.constituent_ID+") " +
							" WHERE n." +net.ddp2p.common.table.news.organization_ID+" IS NULL AND " +
							" n."+net.ddp2p.common.table.news.broadcasted+"<>'0' AND "+
							" n."+net.ddp2p.common.table.news.arrival_date+" > ? LIMIT "+limitNewsLow;
		ArrayList<ArrayList<Object>>p_data = null;
		if(_maxDate[0] == null) {
			p_data = Application.getDB().select( queryNoMaxDate, new String[]{last_sync_date});
		}else{
			p_data = Application.getDB().select( queryMaxDate, new String[]{last_sync_date, _maxDate[0]});
		}
		recentNews.rows = new byte[p_data.size()][][];
		if(DEBUG) out.println("news rows=: "+p_data.size());
		for(int i=0; i<p_data.size(); i++) {
			orgs.add(OrgHandling.ORG_NEWS);
			if(DEBUG) out.print("^");
			byte row[][] = new byte[DD.newsFields.length][];
			row[0] = getFieldData(p_data.get(i).get(0));
			row[1] = getFieldData(p_data.get(i).get(1));
			row[2] = getFieldData(p_data.get(i).get(2));
			row[3] = getFieldData(p_data.get(i).get(3));
			row[4] = getFieldData(p_data.get(i).get(4));
			row[4] = getFieldData(p_data.get(i).get(5));
			recentNews.rows[i] = row;
		}
		return recentNews;
	}
	private static void integrateNewsTable1(Table tab) throws P2PDDSQLException {
		for(int i=0; i<tab.rows.length; i++) {
			String global_news_ID = Util.getBString(tab.rows[i][0]);
			String global_constituentID = Util.getBString(tab.rows[i][1]);
			String date = Util.getBString(tab.rows[i][2]);
			String news = Util.getBString(tab.rows[i][3]);
			String type = Util.getBString(tab.rows[i][4]);
			String signature = Util.getBString(tab.rows[i][5]);
			long constituentID = //UpdateMessages.get_constituent_LID_ByGID(global_constituentID);
					D_Constituent.getLIDFromGID(global_constituentID, -1L);
			if (constituentID < 0) continue;
			long news_ID = UpdateMessages.get_news_ID(global_news_ID, constituentID, -1,  date, news, type, signature);
		}
	}
}
