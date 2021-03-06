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

package net.ddp2p.widgets.org;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

@SuppressWarnings("serial")
class OIDComboBox extends JComboBox implements ActionListener{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public OIDComboBox(){
		setEditable(true);
		//comboBox.setActionCommand("oid_add");
		load();
	}
	@SuppressWarnings("unchecked")
	public void load() {
		this.removeAllItems();
		String sql = "SELECT "+net.ddp2p.common.table.oid.explanation+","+net.ddp2p.common.table.oid.oid_ID+","+net.ddp2p.common.table.oid.OID_name+","+net.ddp2p.common.table.oid.sequence+
		" FROM "+net.ddp2p.common.table.oid.TNAME+";";
		try {
			ArrayList<ArrayList<Object>> oids = Application.getDB().select(sql, new String[]{});
			for(ArrayList<Object> o: oids) {
				addItem(new OIDItem(o.get(2), o.get(1), o.get(0), Util.getString(o.get(3))));
			}
			addItem(new OIDItem("None", null, null, null));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}		
	}
	public Object getOIDItem(String seq) {
		if(seq == null) return null;
		int items = this.getItemCount();
		for(int k = 0; k<items; k++) {
			OIDItem i = (OIDItem)this.getItemAt(k);
			if(i.sequence==null) System.err.println("OIDConboBox:getOIDItem: why null sequence: "+k+" "+i);
			if(seq.equals(i.sequence)) return i;
		}
		return null;
	}
	public static String saveOID(String s){
		if(DEBUG) System.out.println("OIDComboBox: saveOID start");
			if(s==null) return null;
			String[] oidv = s.split("\\(");
			if(oidv.length<2) return null;
			String[] oid = oidv[1].split("\\)");
			try {
				String seq = oid[0].trim();
				if(seq.length()==0) return null;
				ArrayList<ArrayList<Object>> o;
				o = Application.getDB().select("SELECT "+net.ddp2p.common.table.oid.TNAME+" WHERE "+net.ddp2p.common.table.oid.sequence+"=?;", new String[]{seq}, DEBUG);
				if(o.size() != 0) return null;
				Application.getDB().insert(net.ddp2p.common.table.oid.TNAME, 
						new String[]{net.ddp2p.common.table.oid.oid_ID,net.ddp2p.common.table.oid.explanation,
						net.ddp2p.common.table.oid.OID_name, net.ddp2p.common.table.oid.sequence},
						new String[]{seq, oidv[0].trim(), seq, seq}, DEBUG);
				return seq;
			} catch (P2PDDSQLException e1) {
				//e1.printStackTrace();
			}
			return null;
	}
	/**
	 * saves new OIDs, from edits of shape: " explanation ( sequence ) "
	 * @param e
	 */
	public void edit(ActionEvent e){
		if("comboBoxEdited".equals(e.getActionCommand())) {
			if(DEBUG) System.out.println("OrgExtra:action: "+e);
			String s = Util.getString(this.getEditor().getItem());
			if(DEBUG) System.out.println("OrgExtra:action: "+s);
			if(saveOID(s) != null)
				this.load();
		}
	}

}
