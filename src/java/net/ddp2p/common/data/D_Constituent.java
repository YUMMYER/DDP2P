/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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
package net.ddp2p.common.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Motion.D_Motion_Node;
import net.ddp2p.common.handling_wb.PreparedMessage;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.table.field_value;
import net.ddp2p.common.table.identity_ids;
import net.ddp2p.common.table.justification;
import net.ddp2p.common.table.motion;
import net.ddp2p.common.table.news;
import net.ddp2p.common.table.translation;
import net.ddp2p.common.table.witness;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node_Payload;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Summary;
import net.ddp2p.common.util.Util;

public class D_Constituent extends ASNObj  implements  DDP2P_DoubleLinkedList_Node_Payload<D_Constituent>, Summary {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static final int EXPAND_NONE = 0;
	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_ALL = 2;
	
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public int version = 2;
	
	private String global_constituent_id;//Printable
	private String global_constituent_id_hash;
	private String global_submitter_id;//Printable
	private String global_neighborhood_ID;
	private String global_organization_ID;
	
	private String surname;//UTF8
	private String forename;//UTF8
	private String slogan;
	private boolean external;
	public String[] languages;
	public D_FieldValue[] address;
	private String email;//Printable
	private Calendar creation_date;
	private String _creation_date;
	private String weight;
	private boolean revoked;
	private byte[] picture;//OCT STR
	private String hash_alg;//Printable
	private byte[] signature; //OCT STR
	private byte[] certificate; //OCT STR
	
	private D_Witness valid_support;
	private D_Neighborhood[] neighborhood;
	private D_Constituent submitter;

	private String constituent_ID;
	private long _constituent_ID;
	private String submitter_ID;
	private String neighborhood_ID;
	private String organization_ID;
	private long _organization_ID;
	
	
	private D_Peer source_peer;
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private boolean temporary = true;
	private long source_peer_ID;
	private boolean hidden = true;
	D_Constituent_My mydata = new D_Constituent_My();
	private Calendar arrival_date;
	private String _arrival_date;
	private Calendar preferences_date;
	private String _preferences_date;
	
	private int status_references = 0;
	private int status_lock_write = 0; 
	net.ddp2p.ciphersuits.Cipher keys;
	
	public boolean dirty_main = false;
	public boolean dirty_params = false;
	public boolean dirty_locals = true;
	public boolean dirty_mydata = false;
	
	public boolean loaded_globals = false;
	public boolean loaded_locals = false;
	
	private static Object monitor_object_factory = new Object();
	static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	public static final int LOCALIZATION_NAME_EUROPE = 1;
	public static final int LOCALIZATION_NAME_US_F_S = 2;
	public static final int LOCALIZATION_NAME_US_S_F = 0;
	public static int LOCALIZATION_NAME = LOCALIZATION_NAME_US_S_F;
	D_Constituent_Node component_node = new D_Constituent_Node(null, null);
	
	/**
	 * All fields prefixed with alias "c."
	 */
	public static String c_fields_constituents = Util.setDatabaseAlias(net.ddp2p.common.table.constituent.fields_constituents,"c");
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization (to check broadcasting rights).
	 * Could check organization and neighborhood later.
	 */
	public static String sql_get_const =
			"SELECT "+c_fields_constituents+",n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+
			" FROM "+net.ddp2p.common.table.constituent.TNAME+" as c " +
			" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(c."+net.ddp2p.common.table.constituent.neighborhood_ID+" = n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") ";
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+" = o."+table.organization.organization_ID+") ";
	public static String sql_get_consts =
			"SELECT "+c_fields_constituents+",n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+
			" FROM "+net.ddp2p.common.table.constituent.TNAME+" as c " +
			" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(c."+net.ddp2p.common.table.constituent.neighborhood_ID+" = n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "+
			" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(c."+net.ddp2p.common.table.constituent.organization_ID+" = o."+net.ddp2p.common.table.organization.organization_ID+") ";
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization
	 */
	static String sql_get_const_by_ID =
		sql_get_const +
		" WHERE c."+net.ddp2p.common.table.constituent.constituent_ID+" = ?;";
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization
	 * by ID=? OR GID=?
	 */
	static String sql_get_const_by_GID =
			sql_get_const +
			" WHERE c."+net.ddp2p.common.table.constituent.global_constituent_ID+" = ? "+
			" OR c."+net.ddp2p.common.table.constituent.global_constituent_ID_hash+" = ?;";
	static String sql_get_const_by_GID_only =
			sql_get_const +
			" WHERE c."+net.ddp2p.common.table.constituent.global_constituent_ID+" = ?;";
	static String sql_get_const_by_GIDH =
			sql_get_const +
			" WHERE c."+net.ddp2p.common.table.constituent.global_constituent_ID_hash+" = ?;";

	
	public static D_Constituent getEmpty() {return new D_Constituent();}
	public D_Constituent instance() {return new D_Constituent();}
	private D_Constituent() {}
	private D_Constituent(String gID, String gIDH, boolean load_Globals, boolean create, D_Peer __peer, long p_olID) {
		if (DEBUG) System.out.println("D_Constituent: gID="+gID+" gIDH="+gIDH+" glob="+load_Globals+" create="+create+" peer="+__peer);
		ArrayList<ArrayList<Object>> c = null;
		try {
			if (gID != null && gIDH != null) {
				c = Application.getDB().select(sql_get_const_by_GID, new String[]{gID, gIDH}, DEBUG);
			} else if (gID != null) {
				c = Application.getDB().select(sql_get_const_by_GID_only, new String[]{gID}, DEBUG);
			} else if (gIDH != null) {
				c = Application.getDB().select(sql_get_const_by_GIDH, new String[]{gIDH}, DEBUG);
			};
			if (c == null || c.size() == 0) {
				if (! create) throw new D_NoDataException("No such constituent: c_GIDH="+gIDH+" GID="+gID);
				this.source_peer = __peer;
				this.setOrganization(null, p_olID);
				this.setGID(gID, gIDH, p_olID);
				if (__peer != null) this.source_peer_ID = __peer.getLID();
				this.dirty_main = true;
				this.setTemporary();
				//this.storeRequest();
			} else {
				load(c.get(0), EXPAND_NONE);
			}
		} catch (D_NoDataException e) {
			if (DEBUG) e.printStackTrace();
			throw e;//new RuntimeException(e.getMessage());
		} catch (Exception e) {
			if (_DEBUG) e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	private D_Constituent(Long lID, boolean load_Globals) {
		if ((lID == null) || (lID.longValue() <= 0)) throw new RuntimeException("LID="+lID);
		init(Util.getStringID(lID), load_Globals);
	}
	private D_Constituent(String lID, boolean load_Globals) {
		if (lID == null) throw new RuntimeException("LID="+lID);
		init(lID, load_Globals);
	}
	/**
	 * 
	 * @param _forename
	 * @param _surname
	 * @param _email
	 * @param oLID
	 * @param external
	 * @param _ciphersuit
	 * @param _hash_alg
	 * @param _ciphersize
	 * @return
	 */
	public static D_Constituent createConstituent (
			String _forename, String _surname, String _email, 
			long oLID, boolean external, String _weight, String _slogan,
			String _ciphersuit, String _hash_alg, int _ciphersize,
			D_Neighborhood _parentNeighborhood,
			D_Constituent _submitter) {
		D_Constituent new_constituent = D_Constituent.getEmpty();
		
		new_constituent.setForename_dirty(_forename);
		new_constituent.setSurname_dirty(_surname);
		new_constituent.setEmail(_email);
		new_constituent.setExternal_dirty(external);
		new_constituent.setTemporary(false);
		new_constituent.setWeight(_weight);
		new_constituent.setSlogan(_slogan);
		if (_parentNeighborhood != null) {
			new_constituent.setNeighborhoodGID(_parentNeighborhood.getGID());
			new_constituent.setNeighborhood_LID(_parentNeighborhood.getLIDstr());
		}
		if (_submitter != null) {
			new_constituent.setSubmitter(_submitter);
			new_constituent.setSubmitter_ID(_submitter.getLIDstr());
			new_constituent.setSubmitterGID(_submitter.getGID());
		}
		String GID;
		String gcdhash;
		if (external) {
			gcdhash = GID = new_constituent.makeExternalGID();
			new_constituent.setGID(GID, gcdhash, oLID);
			new_constituent.sign();
			//D_Motion.DEBUG = true;
			//String GID = new_constituent.getGID();
		} else {
		   	String now = Util.getGeneralizedTime();
	    	Cipher keys = null;
	    	SK sk; //= ib.getKeys();
//	    	keys = ib.getCipher();
	    	
	    	if (keys == null) {
	    		
	        	try {
					keys = Cipher.mGetStoreCipher(
							_ciphersuit,
							_hash_alg,
							_ciphersize,
							"CST:"+_surname+"_"+_forename,
							"Constituent",
							_email, now);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
	    		sk = keys.getSK();
	    	}
	    	
			GID = Util.getKeyedIDPK(keys);
			String sID = Util.getKeyedIDSK(keys);
			gcdhash = D_Constituent.getGIDHashFromGID_NonExternalOnly(GID);
			String type = Util.getKeyedIDType(keys);
	    	new_constituent.setCreationDate(now);
			new_constituent.sign();
		}
		
		D_Constituent m = D_Constituent.getConstByGID_or_GIDH(GID, gcdhash, true, true, true, null, 
				oLID, new_constituent);
		if (m != new_constituent) {
			m.loadRemote(null, null,new_constituent,  null, false);
			new_constituent = (m);
		}
		
		//new_motion.setBroadcasted(true); // if you sign it, you probably want to broadcast it...
		new_constituent.setTemporary(false);
		new_constituent.setArrivalDate();
		long m_id = new_constituent.storeRequest_getID();
		//new_motion.storeRequest();
		new_constituent.releaseReference();
		return new_constituent;
	}
	public void setForename_dirty(String _forename) {
		this.setForename(_forename);
		this.dirty_main = true;
	}
	public void setSurname_dirty(String _surname) {
		this.setSurname(_surname);
		this.dirty_main = true;
	}
	public void setExternal_dirty(boolean _ext) {
		this.setExternal(_ext);
		this.dirty_main = true;
	}
	public void setExternal(boolean _ext) {
		this.external = _ext;
		this.dirty_main = true;
	}
	private void init(String lID, boolean load_Globals) {
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.getDB().select(sql_get_const_by_ID, new String[]{lID}, DEBUG);
			if (c.size() == 0) throw new RuntimeException("LID="+lID);
			load(c.get(0), EXPAND_NONE);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void load(ArrayList<Object> alk, int _neighborhoods) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_Constituent:load: neigh="+_neighborhoods);		
		constituent_ID = Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_ID));
		_constituent_ID = Util.lval(constituent_ID);
		version = Util.ival(alk.get(net.ddp2p.common.table.constituent.CONST_COL_VERSION), 0);
		setSubmitter_ID(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_SUBMITTER)));
		if (getSubmitter_ID() != null)
			setSubmitterGID(D_Constituent.getGIDFromLID(getSubmitterLID()));

		organization_ID = Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_ORG));
		_organization_ID = Util.lval(organization_ID);
		setOrganizationGID(D_Organization.getGIDbyLID(_organization_ID));
		
		_set_GID(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_GID)));
		setGIDH(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_GID_HASH)));
		if (getGIDH() == null)
			setGIDH(D_Constituent.getGIDHashFromGID(getGID()));
		
		// this.setOrganization(global_organization_ID, _organization_ID);
		
		setSurname(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_SURNAME)));
		//if(DEBUG) System.out.println("ConstituentHandling:load: surname:"+surname+" from="+alk.get(table.constituent.CONST_COL_SURNAME));		
		setForename(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_FORENAME)));
		setWeight(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_WEIGHT)));
		setSlogan(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_SLOGAN)));
		if (DEBUG) System.out.println("D_Constituent:load: external="+alk.get(net.ddp2p.common.table.constituent.CONST_COL_EXTERNAL));
		languages = D_OrgConcepts.stringArrayFromString(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_LANG)));
		address = D_FieldValue.getFieldValues(constituent_ID);		
		setEmail(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_EMAIL)));
		
		setCreationDate(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_DATE_CREATION)));
		setArrivalDate(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_DATE_ARRIVAL)));
		setPreferencesDate(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_DATE_PREFERENCES)));
		
		setNeighborhoodGID(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COLs+0)));
		this.setNeighborhood_LID(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_NEIGH)));
		this.setExternal(Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_EXTERNAL), false));
		this.setRevoked(Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_REVOKED), false));
		this.blocked = Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_BLOCKED),false);
		this.requested = Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_REQUESTED),false);
		this.source_peer_ID = Util.lval(alk.get(net.ddp2p.common.table.constituent.CONST_COL_PEER_TRANSMITTER_ID));
		this.broadcasted = Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		this.hidden = Util.stringInt2bool(alk.get(net.ddp2p.common.table.constituent.CONST_COL_HIDDEN),false);
		
		//picture = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_PICTURE)));
		setPicture(Util.byteSignatureFromString(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_PICTURE))));
		setHash_alg(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_HASH_ALG)));
		String _sgn = Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_SIGNATURE));
		if ("".equals(_sgn)) _sgn = null;
		setSignature(Util.byteSignatureFromString(_sgn));
		//certificate = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_CERTIF)));
		setCertificate(Util.byteSignatureFromString(Util.getString(alk.get(net.ddp2p.common.table.constituent.CONST_COL_CERTIF))));			
		//c.hash = Util.hexToBytes(Util.getString(al.get(k).get(table.constituent.CONST_COL_HASH)).split(Pattern.quote(":")));
		//c.cerReq = al.get(k).get(table.constituent.CONST_COL_CERREQ);
		//c.cert_hash_alg = al.get(k).get(table.constituent.CONST_COL_CERT_HASH_ALG);
				
		
		String my_const_sql = "SELECT "+net.ddp2p.common.table.my_constituent_data.fields_list+
				" FROM "+net.ddp2p.common.table.my_constituent_data.TNAME+
				" WHERE "+net.ddp2p.common.table.my_constituent_data.constituent_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.getDB().select(my_const_sql, new String[]{getLIDstr()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(net.ddp2p.common.table.my_constituent_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(net.ddp2p.common.table.my_constituent_data.COL_CATEGORY));
			mydata.submitter = Util.getString(my_data.get(net.ddp2p.common.table.my_constituent_data.COL_SUBMITTER));
			// skipped preferences_date (used from main)
			mydata.row = Util.lval(my_data.get(net.ddp2p.common.table.my_constituent_data.COL_ROW));
		}
		
		initSK();
		
		loadNeighborhoods(_neighborhoods);
		if (DEBUG) System.out.println("D_Constituent:load: done");		
	}
	
	public long getSubmitterLID() {
		return Util.lval(getSubmitter_ID());
	}
	/**
	 * EXPAND_NONE, EXPAND_ONE, EXPAND_ALL
	 * @param _neighborhoods
	 */
	public void loadNeighborhoods (int _neighborhoods) {
		if (_neighborhoods == EXPAND_NONE || ((getNeighborhoodGID() == null) && (getNeighborhood_LID() == null))) { setNeighborhood(null); return; }
		try {
			setNeighborhood(D_Neighborhood.getNeighborhoodHierarchy(getNeighborhoodGID(), getNeighborhood_LID(), _neighborhoods, this.getOrganizationLID()));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public String toString() {
		String result="D_Constituent: [ #"+this.constituent_ID;
		result += "\n version="+version;
		result += "\n orgGID=["+organization_ID+","+this._organization_ID+"]="+getOrganizationGID();
		result += "\n costGID="+getGID();
		result += "\n constGIDH="+getGIDH();
		result += "\n surname="+getSurname();
		result += "\n forename="+getForename();
		result += "\n address="+Util.nullDiscrim(address, Util.concat(address,":"));
		result += "\n email="+getEmail();
		result += "\n crea_date="+Encoder.getGeneralizedTime(creation_date);
		result += "\n neigGID="+getNeighborhoodGID();
		result += "\n picture="+Util.nullDiscrim(getPicture(), Util.stringSignatureFromByte(getPicture()));
		result += "\n hash_alg="+getHash_alg();
		result += "\n certif="+Util.nullDiscrim(getCertificate(), Util.stringSignatureFromByte(getCertificate()));
		result += "\n lang="+Util.nullDiscrim(languages, Util.concat(languages,":"));
		result += "\n submitterGID="+getSubmitterGID();
		result += "\n submitter="+getSubmitter();
		result += "\n slogan="+getSlogan();
		result += "\n weight="+getWeight();
		result += "\n external="+isExternal();
		result += "\n revoked="+isRevoked();
		result += "\n signature="+Util.byteToHexDump(getSignature());
		result += "\n  row="+this.mydata.row;
		result += "\n  name="+this.mydata.name;
		result += "\n  submiter="+this.mydata.submitter;
		result += "\n  category="+this.mydata.category;
		if(getNeighborhood()!=null) result += "\n neigh=["+Util.concat(getNeighborhood(), "\n\n")+"]";
		return result+"]";
	}
	public String toSummaryString() {
		String result="D_Constituent: [";
		if (getOrganizationGID() != null) result += "\n orgGID="+getOrganizationGID();
		result += "\n surname="+getSurname()+" [#"+this.mydata.row+" "+this.mydata.name+"]";
		result += "\n forename="+getForename();
		if(getNeighborhood()!=null) result += "\n neigh=["+Util.concatSummary(getNeighborhood(), "\n\n", null)+"]";
		return result+"]";
	}

	public static class D_Constituent_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Constituent> loaded_objects = new DDP2P_DoubleLinkedList<D_Constituent>();
		private static Hashtable<Long, D_Constituent> loaded_By_LocalID = new Hashtable<Long, D_Constituent>();
		//private static Hashtable<String, D_Constituent> loaded_const_By_GID = new Hashtable<String, D_Constituent>();
		//private static Hashtable<String, D_Constituent> loaded_const_By_GIDhash = new Hashtable<String, D_Constituent>();
		private static Hashtable<String, Hashtable<Long, D_Constituent>> loaded_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Constituent>>();
		private static Hashtable<Long, Hashtable<String, D_Constituent>> loaded_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Constituent>>();
		private static Hashtable<String, Hashtable<Long, D_Constituent>> loaded_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Constituent>>();
		private static Hashtable<Long, Hashtable<String, D_Constituent>> loaded_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Constituent>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Constituent getConstByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Constituent> t1 = loaded_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Constituent> t2 = loaded_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Constituent putByGID(String GID, Long organizationLID, D_Constituent c) {
			Hashtable<Long, D_Constituent> v1 = loaded_By_GID_ORG.get(GID);
			if (v1 == null) loaded_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Constituent>());

			// section added for duplication control
			D_Constituent old = v1.get(organizationLID);
			if (old != null && old != c) {
				Util.printCallPath("D_Constituent conflict: old="+old+" crt="+c);
				return old;
			}
			
			D_Constituent result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Constituent> v2 = loaded_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Constituent>());
			
			// section added for duplication control
			old = v2.get(GID);
			if (old != null && old != c) {
				Util.printCallPath("D_Constituent conflict: old="+old+" crt="+c);
				//return old; // in this case, for consistency, store the new one
			}
			
			D_Constituent result2 = v2.put(GID, c);
			
			//if (result == null) result = result2;
			return c; //result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Constituent remByGID(String GID, Long organizationLID) {
			D_Constituent result = null;
			D_Constituent result2 = null;
			Hashtable<Long, D_Constituent> v1 = loaded_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Constituent> v2 = loaded_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Constituent getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Constituent getByGIDH(String GIDH, Long organizationLID) {
			if (GIDH == null) {
				Util.printCallPath("Why calling this with null?");
				return null;
			}
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Constituent> t1 = loaded_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Constituent> t2 = loaded_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Constituent putByGIDH(String GIDH, Long organizationLID, D_Constituent c) {
			Hashtable<Long, D_Constituent> v1 = loaded_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Constituent>());
			
			// section added for duplication control
			D_Constituent old = v1.get(organizationLID);
			if (old != null && old != c) {
				Util.printCallPath("D_Constituent conflict: old="+old+" crt="+c);
				return old;
			}
			
			D_Constituent result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Constituent> v2 = loaded_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Constituent>());
			
			// section added for duplication control
			old = v2.get(GIDH);
			if (old != null && old != c) {
				Util.printCallPath("D_Constituent conflict: old="+old+" crt="+c);
				//return old; // in this case, for consistency, store the new one
			}
			
			D_Constituent result2 = v2.put(GIDH, c);
			
			//if (result == null) result = result2;
			return c; //result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Constituent remByGIDH(String GIDH, Long organizationLID) {
			D_Constituent result = null;
			D_Constituent result2 = null;
			Hashtable<Long, D_Constituent> v1 = loaded_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Constituent> v2 = loaded_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Constituent getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Constituent> my_node_in_loaded;
	
		public D_Constituent_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Constituent> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Constituent crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if (crt.component_node.message != null) return;
			if (!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.temporary)) {
				byte[] message = crt.encode();
				synchronized (loaded_objects) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/**
		 * This function is used to link an object by its LID when this is obtained
		 * by storing an object already linked by its GIDH (if it was linked)
		 * @param crt
		 * @return
		 * true if i is linked and false if it is not
		 */
		private static boolean register_newLID_ifLoaded(D_Constituent crt) {
			if (DEBUG) System.out.println("D_Constituent: register_newLID_ifLoaded: start crt = "+crt);
			synchronized (loaded_objects) {
				//String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				if (gidh == null) {
					if (_DEBUG) { System.out.println("D_Constituent: register_newLID_ifLoaded: had no gidh! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				if (lid <= 0) {
					Util.printCallPath("Why call without LID="+crt);
					return false;
				}
				
				Long organizationLID = crt.getOrganizationLID();
				if (organizationLID <= 0) {
					Util.printCallPath("No orgLID="+crt);
					return false;
				}
				D_Constituent old = getByGIDH(gidh, organizationLID);
				if (old == null) {
					if (DEBUG) System.out.println("D_Constituent: register_newLID_ifLoaded: was not registered.");
					return false;
				}
				
				if (old != crt)	{
					Util.printCallPath("Different linking of: old="+old+" vs crt="+crt);
					return false;
				}
				
				Long oLID = new Long(lid);
				D_Constituent _old = loaded_By_LocalID.get(oLID);
				if (_old != null && _old != crt) {
					Util.printCallPath("Double linking of: old="+_old+" vs crt="+crt);
					return false;
				}
				loaded_By_LocalID.put(oLID, crt);
				if (DEBUG) System.out.println("D_Constituent: register_newLID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
		private static boolean register_newGID_ifLoaded(D_Constituent crt) {
			if (DEBUG) System.out.println("D_Constituent: register_newGID_ifLoaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				if (gidh == null) {
					Util.printCallPath("Why call without GIDH="+crt);
					return false;
				}
				if (lid <= 0) {
					if (_DEBUG) { System.out.println("D_Constituent: register_newGID_ifLoaded: had no lid! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				
				Long organizationLID = crt.getOrganizationLID();
				if (organizationLID <= 0) {
					Util.printCallPath("No orgLID="+crt);
					return false;
				}
				
				Long oLID = new Long(lid);
				D_Constituent _old = loaded_By_LocalID.get(oLID);
				if (_old == null) {
					if (DEBUG) System.out.println("D_Constituent: register_newGID_ifLoaded: was not loaded");
					return false;
				}
				if (_old != null && _old != crt) {
					Util.printCallPath("Using expired: old="+_old+" vs crt="+crt);
					return false;
				}
				
				D_Constituent_Node.putByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
				if (DEBUG) System.out.println("D_Constituent: register_newGID_ifLoaded: store gid="+gid);
				D_Constituent_Node.putByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
				if (DEBUG) System.out.println("D_Constituent: register_newGID_ifLoaded: store gidh="+gidh);
				
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				if (DEBUG) System.out.println("D_Constituent: register_newGID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
		/*
		private static void unregister_loaded(D_Constituent crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static boolean register_loaded(D_Constituent crt) {
			if (DEBUG) System.out.println("D_Constituent: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrganizationLID();
				
				loaded_objects.offerFirst(crt);
				if (lid > 0) {
					// section added for duplication control
					Long oLID = new Long(lid);
					D_Constituent old = loaded_By_LocalID.get(oLID);
					if (old != null && old != crt) {
						Util.printCallPath("Double linking of: old="+old+" vs crt="+crt);
						//return false;
					}
					
					loaded_By_LocalID.put(oLID, crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Constituent_Node.putByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Constituent_Node.putByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_objects.size() > SaverThreadsConstants.MAX_LOADED_CONSTS)
						|| (current_space > SaverThreadsConstants.MAX_CONSTS_RAM)) {
					if (loaded_objects.size() <= SaverThreadsConstants.MIN_LOADED_CONSTS) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Constituent candidate = loaded_objects.getTail();
					if ((candidate.get_StatusReferences() + candidate.get_StatusLockWrite() > 0)
							||
							D_Constituent.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Constituent removed = loaded_objects.removeTail();//remove(loaded_peers.size()-1);
					loaded_By_LocalID.remove(new Long(removed.getLID())); 
					D_Constituent_Node.remByGID(removed.getGID(), removed.getOrganizationLID());//loaded_const_By_GID.remove(removed.getGID());
					D_Constituent_Node.remByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Constituent: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
				return true;
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Constituent crt) {
			loaded_objects.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Constituent removed, boolean force) {
			boolean result = true;
			synchronized(loaded_objects) {
				if (removed.get_StatusLockWrite() > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Constituent: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				if (loaded_objects.inListProbably(removed)) {
					try {
						loaded_objects.remove(removed);
						if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
						if (DEBUG) System.out.println("D_Constituent: dropLoaded: exit with force="+force+" result="+result);
					} catch (Exception e) {
						if (_DEBUG) e.printStackTrace();
					}
				}
				if (removed.getLIDstr() != null) loaded_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Constituent_Node.remByGID(removed.getGID(), removed.getOrganizationLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Constituent_Node.remByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Constituent: drop_loaded: remove GIDH="+removed.getGIDH());
				return result;
			}
		}
	}
	static class D_Constituent_My {
		String name;// table.my_organization_data.
		String category;
		String submitter;
		Calendar preference_date;
		String _preference_date;
		long row;
	}

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static private D_Constituent getConstByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Constituent crt = D_Constituent_Node.loaded_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Constituent_Node.register_fully_loaded(crt);
		}
		D_Constituent_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Constituent getConstByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Constituent  crt = getConstByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.inc_StatusLockWrite();
					if (crt.get_StatusLockWrite() > 1) {
						System.out.println("D_Constituent: getOrgByGIDhash_AttemptCacheOnly: "+crt.get_StatusLockWrite());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getConstByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}

	static public D_Constituent getConstByLID(String LID, boolean load_Globals, boolean keep) {
		return getConstByLID(Util.Lval(LID), load_Globals, keep);
	}
	static public D_Constituent getConstByLID(Long LID, boolean load_Globals, boolean keep) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Constituent: getConstByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Constituent crt = D_Constituent.getConstByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Constituent.getConstByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Constituent(LID, load_Globals);
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: loaded crt="+crt);
				D_Constituent_Node.register_loaded(crt);
				if (keep) {
					crt.inc_StatusLockWrite();
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: Done");
			return crt;
		}
	}	
	
	
	/**
	 * No keep, throws exception if GIDH is invalid!
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static private D_Constituent getConstByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, Long organizationLID, boolean load_Globals) {
		D_Constituent  crt = null;
		if ((GID == null) && (GIDhash == null)) return null;
		if (GIDhash != null) crt = D_Constituent_Node.getByGIDH(GIDhash, organizationLID);//.loaded_const_By_GIDhash.get(GIDhash);
		if ((crt == null) && (GID != null)) crt = D_Constituent_Node.getConstByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
		
		if ((GID != null) && ((crt == null) || (GIDhash == null) || DD.VERIFY_GIDH_ALWAYS)) {
			String hash = D_Constituent.getGIDHashFromGID(GID);
			if (hash == null) {
				System.out.println("D_Constituent: getOrgByGID_or_GIDhash_Attempt: fail to get GIDH from "+GID+" to compare with: "+GIDhash );
				throw new RuntimeException("No GIDhash computation possible");
			}
			if (GIDhash != null) {
				if (! hash.equals(GIDhash)) {
					System.out.println("D_Constituent: getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
					throw new RuntimeException("No GID and GIDhash match");
				}
			} else {
				GIDhash = hash;
			}
			if (crt == null) crt = D_Constituent_Node.getByGIDH(GIDhash, organizationLID); //.loaded_const_By_GIDhash.get(GIDhash);
		}
		
		if (crt != null) {
			crt.setGID(GID, GIDhash, crt.getOrganizationLID());
			
			if (load_Globals && ! crt.loaded_globals) {
				crt.fillGlobals();
				D_Constituent_Node.register_fully_loaded(crt);
			}
			D_Constituent_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	/*
	static private D_Constituent getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Constituent  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					//System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
					//Util.printCallPath("");
				}
				return crt;
			}
		} else {
			return getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
		}
	}
	*/
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Constituent getConstByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null) && (GIDhash == null)) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Constituent  crt = getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, oID, load_Globals);
				if (crt != null) {			
					crt.inc_StatusLockWrite();
					if (crt.get_StatusLockWrite() > 1) {
						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.get_StatusLockWrite());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, oID, load_Globals);
		}
	}
	/**
	 * Only attempts too load (only based on GID) if the data is already in the cache
	 * exception raised on error
	 * @param GID
	 * @param load_Globals 
	 * @return
	 */
	/*
	static private D_Constituent getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Constituent crt = D_Constituent_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Constituent_Node.register_fully_loaded(crt);
		}
		D_Constituent_Node.setRecent(crt);
		return crt;
	}
	*/
	@Deprecated
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean keep) {
		System.out.println("D_Constituent: getConstByGID_or_GIDH: Remove me setting orgID");
		
		return getConstByGID_or_GIDH(GID, GIDH, load_Globals, false, keep, null, -1);
	}
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean keep, Long oID) {
		return getConstByGID_or_GIDH(GID, GIDH, load_Globals, false, keep, null, oID);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * @param GID
	 * @param GIDH
	 * @param oID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @return
	 */
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean create, boolean keep, D_Peer __peer, long p_oLID) {
		return getConstByGID_or_GIDH(GID, GIDH, load_Globals, create, keep, __peer, p_oLID, null);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * @param GID
	 * @param GIDH
	 * @param oID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @return
	 */
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean create, boolean keep, D_Peer __peer, long p_oLID, D_Constituent storage) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: "+GID+", GIDH="+GIDH+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if (create) {
			if (!keep) Util.printCallPath("Why");
			keep = true;
		}
		if ((GID == null) && (GIDH == null)) {
			Util.printCallPath("Why null");
			if (_DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: null GID and GIDH");
			return null;
		}

		if ((GIDH != null) && ! D_Constituent.isGIDHash(GIDH)) {
			if (GID == null) GID = GIDH;
			GIDH = D_Constituent.getGIDHashFromGID(GIDH);
		}
		
		D_Constituent crt = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDH, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDH, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: got sync cached crt="+crt);
				return crt;
			}

			try {
				try {
					crt = new D_Constituent(GID, GIDH, load_Globals, create && (storage == null), __peer, p_oLID);
				} catch (D_NoDataException e) {
					if (! create || (storage == null)) throw e;
					//if (storage != null) 
					{
						crt = storage;
						crt.source_peer = __peer;
						crt.setOrganization(null, p_oLID);
						crt.setGID(GID, GIDH, p_oLID);
						if (__peer != null) crt.source_peer_ID = __peer.getLID();
						crt.dirty_main = true;
						crt.setTemporary();
					}
				}
				
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: loaded crt="+crt);
				if (keep) {
					crt.inc_StatusLockWrite();
				}
				D_Constituent_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: Done");
			return crt;
		}
	}	
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Constituent getConstByConst_Keep(D_Constituent constit) {
		if (constit == null) return null;
		D_Constituent result = D_Constituent.getConstByGID_or_GIDH(constit.getGID(), constit.getGIDH(), true, true, constit.getOrganizationLID());
		if (result == null) {
			result = D_Constituent.getConstByLID(constit.getLID(), true, true);
		}
		if (result == null) {
			if ((constit.getLIDstr() == null) && (constit.getGIDH() == null)) {
				result = constit;
				{
					constit.inc_StatusLockWrite();
					
					System.out.println("D_Constituent: getConstByConst_Keep: "+constit.get_StatusLockWrite());
					Util.printCallPath("Why: constit="+constit);
				}
			}
		}
		if (result == null) {
			System.out.println("D_Constituent: getConstByConst_Keep: got null for "+constit);
			Util.printCallPath("");
		}
		return result;
	} 

	
	/** Storing */
	public static D_Constituent_SaverThread saverThread = new D_Constituent_SaverThread();
	public boolean dirty_any() {
		return dirty_main || dirty_params || dirty_locals || dirty_mydata;
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		// if (this.constituent_ID == null) Util.printCallPath("Why store null LID: "+this);
		// else Util.printCallPath("Why store nonull LID: "+this);
		
		if (! this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}

		if (this.arrival_date == null && (this.getSignature() != null && this.getSignature().length > 0)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Constituent: storeRequest: missing arrival_date");
			Util.printCallPath("D_Constituent: storeRequest: Why no arrival time??");
		}
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Constituent._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Peer:storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Organization:storeRequest: GIDH="+save_key);
			D_Constituent._need_saving.add(this);
		}
		try {
			if (!saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_Peer:storeRequest:startThread");
				saverThread.start();
			}
			synchronized(saverThread) {saverThread.notify();}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * dirty_instances - ...
	 * dirty_served_orgs - ...
	 * dirty_my_data - ...
	 * dirty_addresses - ...
	 * 
	 * @return This returns synchronously (waiting for the storing to happen), and returns the local ID.
	 */
	public long storeRequest_getID() {
		if ( this.getLIDstr() == null ) 
			return this.storeSynchronouslyNoException();
		this.storeRequest();
		return this.getLID();
	}
	/**
	 * This can be delayed saving
	 * @return
	 */
	public long storeSynchronouslyNoException() {
		try {
			return storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	/**
	 * The next monitor is used for the storeAct, to avoid concurrent modifications of the same object.
	 * Potentially the monitor can be a field in the same object (since saving of different objects
	 * is not considered dangerous, even when they are of the same type)
	 * 
	 * What we do is the equivalent of a synchronized method "storeAct" that we avoid to avoid accidental synchronization
	 * with other methods.
	 */
	final Object monitor = new Object();
	//static final Object monitor = new Object();
	
	public long storeAct() throws P2PDDSQLException {
		synchronized(monitor) {
			return _storeAct();
		}
	}
	/**
	 * This is not synchronized
	 * @return
	 * @throws P2PDDSQLException
	 */
	private long _storeAct() throws P2PDDSQLException {
		boolean sync = true; 
		D_Organization org = D_Organization.getOrgByLID(this.organization_ID, true, false);
		if (org == null) {
			if (_DEBUG) System.out.print("D_Constituent: storeAct: no org "+this);
			return -1;
		}
		if (DEBUG) System.out.print("."+this.constituent_ID+".");
		if (this.dirty_locals || this.dirty_main || (this.getLID() <= 0)) {
			this.dirty_locals = this.dirty_main = false;
			storeAct_main(sync);
		}
		if (this.dirty_params) {
			this.dirty_params = false;
			try {
				D_FieldValue.store(sync, address, constituent_ID, this.getOrganizationLID(), DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS, org);
			} catch (ExtraFieldException e) {
				e.printStackTrace();
			}
		}
		if (this.dirty_mydata) {
			this.dirty_mydata = false;
			storeAct_my(sync);
		}
		if (DEBUG) System.out.println("ConstituentHandling:storeVerified: return="+_constituent_ID);
		return _constituent_ID;
	}
	private long storeAct_main(boolean sync) throws P2PDDSQLException {
		// Util.printCallPath("sync="+sync+" this="+this);
		if (this.arrival_date == null && (this.getSignature() != null && this.getSignature().length > 0)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Constituent: missing arrival_date");
			Util.printCallPath("D_Constituent: storeRequest: Why no arrival time??");
		}
			
		//String[] fields = table.constituent.fields_constituents_no_ID_list;
		String[] params = new String[(constituent_ID != null) ?
				net.ddp2p.common.table.constituent.CONST_COLs:
					net.ddp2p.common.table.constituent.CONST_COLs_NOID];
		//params[table.constituent.CONST_COL_ID] = ;
		params[net.ddp2p.common.table.constituent.CONST_COL_GID] = getGID();
		params[net.ddp2p.common.table.constituent.CONST_COL_GID_HASH] = this.getGIDH();
		params[net.ddp2p.common.table.constituent.CONST_COL_SURNAME] = getSurname();
		params[net.ddp2p.common.table.constituent.CONST_COL_FORENAME] = getForename();
		params[net.ddp2p.common.table.constituent.CONST_COL_SLOGAN] = getSlogan();
		params[net.ddp2p.common.table.constituent.CONST_COL_WEIGHT] = getWeight();
		params[net.ddp2p.common.table.constituent.CONST_COL_EXTERNAL] = Util.bool2StringInt(isExternal());
		params[net.ddp2p.common.table.constituent.CONST_COL_REVOKED] = Util.bool2StringInt(isRevoked());
		params[net.ddp2p.common.table.constituent.CONST_COL_VERSION] = ""+version;
		params[net.ddp2p.common.table.constituent.CONST_COL_LANG] = D_OrgConcepts.stringFromStringArray(languages);
		params[net.ddp2p.common.table.constituent.CONST_COL_EMAIL] = getEmail();
		params[net.ddp2p.common.table.constituent.CONST_COL_PICTURE] = Util.stringSignatureFromByte(getPicture());
		params[net.ddp2p.common.table.constituent.CONST_COL_DATE_CREATION] = _creation_date; //Encoder.getGeneralizedTime(creation_date);
		params[net.ddp2p.common.table.constituent.CONST_COL_DATE_ARRIVAL] = getArrivalDateStr();
		params[net.ddp2p.common.table.constituent.CONST_COL_DATE_PREFERENCES] = getPreferencesDateStr();
		params[net.ddp2p.common.table.constituent.CONST_COL_NEIGH] = this.getNeighborhood_LID();
		params[net.ddp2p.common.table.constituent.CONST_COL_PEER_TRANSMITTER_ID] = Util.getStringID(this.source_peer_ID);
		params[net.ddp2p.common.table.constituent.CONST_COL_HASH_ALG] = getHash_alg();
		params[net.ddp2p.common.table.constituent.CONST_COL_SIGNATURE] = Util.stringSignatureFromByte(getSignature());
		params[net.ddp2p.common.table.constituent.CONST_COL_CERTIF] = Util.stringSignatureFromByte(getCertificate());
		params[net.ddp2p.common.table.constituent.CONST_COL_ORG] = this.organization_ID;
		params[net.ddp2p.common.table.constituent.CONST_COL_SUBMITTER] = getSubmitter_ID();
		params[net.ddp2p.common.table.constituent.CONST_COL_OP] = "1";
		params[net.ddp2p.common.table.constituent.CONST_COL_BLOCKED] = Util.bool2StringInt(blocked);
		params[net.ddp2p.common.table.constituent.CONST_COL_REQUESTED] = Util.bool2StringInt(requested);
		params[net.ddp2p.common.table.constituent.CONST_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
		params[net.ddp2p.common.table.constituent.CONST_COL_HIDDEN] = Util.bool2StringInt(this.hidden);

		try {
			if (this.constituent_ID == null) {
				if (this.getOrganizationLIDstr() != null) {
					D_Organization o = D_Organization.getOrgByLID_AttemptCacheOnly_NoKeep(this.getOrganizationLID(), false);
					if (o != null) o.resetCache(); // removing cached memory of statistics about the constituent!
				}
//				if (this.getLIDstr() != null) {
//					D_Constituent c = D_Constituent.getConstByLID_AttemptCacheOnly(this.getLID(), false);
//					if (c != null) c.resetCache(); // removing cached memory of statistics about justifications!
//				}
					
				setLID_AndLink(Application.getDB().insert(sync, net.ddp2p.common.table.constituent.TNAME,
						net.ddp2p.common.table.constituent.fields_constituents_no_ID_list, params, DEBUG));
					
			} else {
				params[net.ddp2p.common.table.constituent.CONST_COL_ID] = constituent_ID;
				Application.getDB().update(sync, net.ddp2p.common.table.constituent.TNAME,
						net.ddp2p.common.table.constituent.fields_constituents_no_ID_list,
						new String[]{net.ddp2p.common.table.constituent.constituent_ID}, params, DEBUG);
					
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("D_Constituent: store_main: exit: "+_constituent_ID);
		return _constituent_ID;
	}
	private void storeAct_my(boolean sync) {
		String param[];
		if (this.mydata.row <= 0) {
			param = new String[net.ddp2p.common.table.my_constituent_data.FIELDS_NB_NOID];
		} else {
			param = new String[net.ddp2p.common.table.my_constituent_data.FIELDS_NB];
		}
		param[net.ddp2p.common.table.my_constituent_data.COL_NAME] = this.mydata.name;
		param[net.ddp2p.common.table.my_constituent_data.COL_CATEGORY] = this.mydata.category;
		param[net.ddp2p.common.table.my_constituent_data.COL_SUBMITTER] = this.mydata.submitter;
		param[net.ddp2p.common.table.my_constituent_data.COL_CONSTITUENT_LID] = this.constituent_ID;
		try {
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.getDB().insert(sync, net.ddp2p.common.table.my_constituent_data.TNAME,
								net.ddp2p.common.table.my_constituent_data.fields_noID, param, DEBUG);
			} else {
				param[net.ddp2p.common.table.my_constituent_data.COL_ROW] = this.mydata.row+"";
				Application.getDB().update(sync, net.ddp2p.common.table.my_constituent_data.TNAME,
						net.ddp2p.common.table.my_constituent_data.fields_noID,
						new String[]{net.ddp2p.common.table.my_constituent_data.row}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public long storeRemoteThis(PreparedMessage pm, 
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if (DEBUG) System.out.println("ConstituentHandling:storeVerified: start");
		long result;
		this._organization_ID = D_Organization.getLIDbyGID(this.getOrgGID());
		if (_organization_ID <= 0) {
			System.out.println("D_Constituent: storeRemoteThis: unknown organization: "+this.getOrgGID());
			return -1;
		}
		if ( ( getNeighborhood() != null) && ( getNeighborhood().length > 0 ) ) {
			for (int k = 0; k < getNeighborhood().length; k ++) {
				getNeighborhood()[k].storeRemoteThis(this.getOrgGID(), this._organization_ID, this.getArrivalDateStr(), sol_rq, new_rq, __peer);
			}
		}
		D_Constituent c = D_Constituent
				.getConstByGID_or_GIDH(this.getGID(), this.getGIDH(), true, true, true, __peer, this._organization_ID);
		c.loadRemote(null, null, this, __peer, false);
		c.storeRequest();
		c.releaseReference();
		result = c.getLID_force();
		if(sol_rq!=null)sol_rq.cons.put(this.getGIDH(), DD.EMPTYDATE);
		return result;
//		if (this._constituent_ID <= 0) result = this.storeSynchronouslyNoException();
//		else {
//			D_Constituent c = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(getGID(), getGIDH(), this.getOrganizationID(), true, true);
//			c.storeRequest();
//			c.releaseReference();
//			result = this._constituent_ID;
//		}
//		if(result > 0) if(sol_rq!=null)sol_rq.cons.put(this.global_constituent_id_hash, DD.EMPTYDATE);
//		return result;		
	}
			/**
			 * Not needed when there is no concurrency!
			 */
			/*
			synchronized(D_Constituent.monitor){
				if(DEBUG) System.out.print(";"+this.constituent_ID+";");
				if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
					this.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
					if(DEBUG) System.out.println("D_Constituent:storeVerified: late reget id=="+this.constituent_ID+" for:"+this.getName());
					if(this.constituent_ID != null){
						params = Util.extendArray(params, table.constituent.CONST_COLs);
						params[table.constituent.CONST_COL_ID] = constituent_ID;
						this._constituent_ID = Util.lval(this.constituent_ID, -1);
					}
				}
				
				if(DEBUG) System.out.println(":"+this.constituent_ID+":");
				if(this.constituent_ID==null){
					if(DEBUG) System.out.println("ConstituentHandling:storeVerified: insert!");
					try{
						_constituent_ID=Application.db.insert(sync, table.constituent.TNAME,
								table.constituent.fields_constituents_no_ID_list, params, DEBUG);
					}catch(Exception e){
						if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed hash="+global_constituent_id_hash);
						e.printStackTrace();
						if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
							this.constituent_ID = D_Constituent_D.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
							if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed reget id=="+this.constituent_ID);
						}
						this._constituent_ID = Util.lval(this.constituent_ID, -1);
					}
					constituent_ID = Util.getStringID(_constituent_ID);
					if(constituent_ID==null) return _constituent_ID;
				}else {
					if(DEBUG) System.out.println("ConstituentHandling:storeVerified: update!");
					//params[table.constituent.CONST_COLs] = constituent_ID;
					//params[table.constituent.CONST_COL_ID] = constituent_ID;
					if ((date[0]==null)||(date[0].compareTo(params[table.constituent.CONST_COL_DATE_CREATION])<0)) {
						params[params.length-1] = constituent_ID;
						Application.db.update(sync, table.constituent.TNAME,
								table.constituent.fields_constituents_no_ID_list, new String[]{table.constituent.constituent_ID}, params, DEBUG);
					} else {
						if (DEBUG) System.out.println("ConstituentHandling:storeVerified: not new data vs="+date[0]);				
					}
					_constituent_ID = new Integer(constituent_ID).longValue();
				}
				if (DEBUG) System.out.println("ConstituentHandling:storeVerified: stored constituent!");
			*/
				/*
				if ( (neighborhood != null ) && ( neighborhood.length > 0 ) ) {
					for (int k = 0; k < neighborhood.length; k ++) {
						neighborhood[k].store(sync, orgGID, org_local_ID, arrival_time, sol_rq, new_rq);
					}
				}
				*/
			/*
				if(DEBUG) System.out.println("ConstituentHandling:storeVerified: store address!");
				//long _organization_ID = Util.lval(org_local_ID, -1);
				try {
					D_FieldValue.store(sync, address, constituent_ID, this.getOrganizationID(), DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS);
				} catch (ExtraFieldException e) {
					Application.db.update(sync, table.constituent.TNAME, new String[]{table.constituent.sign},
							new String[]{table.constituent.constituent_ID},
							new String[]{null, constituent_ID}, DEBUG);
					e.printStackTrace();
					Application_GUI.warning(__("Extra Field Type for constituent.")+" "+forename+", "+surname+"\n"+
							__("Constituent dropped:")+" "+e.getLocalizedMessage(),
							__("Unknow constituent info dropped"));
				}
			}
		}
				*/
	public void setLID_AndLink(long _constituentLID) {
		setConstituentLID(_constituentLID);
		if (_constituentLID > 0)
			D_Constituent_Node.register_newLID_ifLoaded(this);
	}
	public void setConstituentLID(long _constituentLID) {
		this._constituent_ID = _constituentLID;
		this.constituent_ID = Util.getStringID(_constituent_ID);
	}
	public long getOrganizationLID() {
		return this._organization_ID;
	}
	public String getOrganizationLIDstr() {
		return this.organization_ID;
	}
	public PK getPK() {
		if (this.isExternal()) return null;
		//if (stored_pk != null) return stored_pk;
		//SK sk = getSK();
		if (keys != null) return keys.getPK();
		PK pk = net.ddp2p.ciphersuits.Cipher.getPK(getGID());
		//stored_pk = pk;
		keys = Cipher.getCipher(pk);
		return pk;
	}
	public boolean isExternal() {
		return this.external;
	}
	public String getLIDstr() {
		return this.constituent_ID;
	}
	public long getLID() {
		return this._constituent_ID;
	}
	public long getLID_force() {
		if (this._constituent_ID > 0) return this._constituent_ID;
		return this.storeSynchronouslyNoException();
	}
	public String getGID() {
		return global_constituent_id;
	}
	public String _set_GID(String global_constituent_id) {
		if (! D_GIDH.isGID(global_constituent_id)) {
			Util.printCallPath("Why:" + global_constituent_id);
		}
		this.global_constituent_id = global_constituent_id;
		return global_constituent_id;
	}
	public void setGID_AndLink (String gID, String gIDH, Long oID) {
		setGID(gID, gIDH, oID);
		if (this.getGID() != null) // && isLoaded())
			D_Constituent_Node.register_newGID_ifLoaded(this);
	}	
	public void setGID (String gID, String gIDH, Long oID) {
		//boolean loaded_in_cache = this.isLoaded();
		
		String oldGID = this.getGID();
		String oldGIDH = this.getGIDH();

		// sanitize input
		if (gID != null) {
			if (D_GIDH.isCompactedGID(gID)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gID = null;
			}
		}
		if (gIDH != null) {
			if (D_GIDH.isCompactedGID(gIDH)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gIDH = null;
			}
		}
		// sanitize GIDH
		if (gIDH != null) {
			if (! D_GIDH.isGIDH(gIDH)) {
				Util.printCallPath("Should be a GIDH: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gIDH = null;
			}
		}
		// sanitize input: infer a gidh if possible from GID
		if ((gID != null) && (gIDH == null)) {
			if (D_GIDH.isGID(gID)) {
				gIDH = D_Constituent.getGIDHashFromGID(gID);
			} else {
				if (D_GIDH.isGIDH(gID)) {
					gIDH = gID;
				} else {
					Util.printCallPath("Why? GID is nothing: gID="+gID+" IDH="+gIDH);
				}
				//gID = null; // done later
			}
			if (gIDH == null) Util.printCallPath("D_Constituent: null GIDH when setGID:"+gID+" for: "+this);
		}
		// sanitize  remove non gid
		if (gID != null) {
			if (! D_GIDH.isGID(gID)) {
//				if (!this.isExternal())
				Util.printCallPath("Should be a GID: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gID = null;
			}
		}
		
		if ( (gID != null) && ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (DEBUG && oldGID != null) Util.printCallPath("Why: new GID="+gID+" vs oldGID="+oldGID);
			if (oldGID != null) {
				if (D_GIDH.isGID(oldGIDH) && !D_GIDH.isGID(gID)) {
					// dead code: should never be here since we tested before
					gID = oldGIDH;
				} else {
					//D_Constituent_Node.remByGID(oldGID, oID); //.loaded_const_By_GID.remove(oldGID);
					this._set_GID(gID);
					this.dirty_main = true;
				}
			} else {
				this._set_GID(gID);
				this.dirty_main = true;
			}
		}
				
		// handle GIDH
		if (gIDH != null) {
			if ( ! Util.equalStrings_null_or_not(oldGIDH, gIDH)) {		
				if (oldGIDH != null) {
					//D_Constituent_Node.remByGIDH(this.getGIDH(), oID); //.loaded_const_By_GIDhash.remove(this.getGIDH());
					if (DEBUG) Util.printCallPath("Why? change to ID="+gID+" IDH="+gIDH);
				}
				this.setGIDH(gIDH);
				this.dirty_main = true;
			}
		}		
		
//		if (loaded_in_cache) {
//			if (this.getGID() != null)
//				D_Constituent_Node.putByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
//			if (this.getGIDH() != null)
//				D_Constituent_Node.putByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
//		}
	}
	
	/**
	 * Useful if there was an old gid different from the new one!
	 * 
	 * Older implementation which is directly removing the old GID from the loaded data structures.
	 * Probably that is not done in the new implementation that is used!!!
	 * This is why this is not deleted, to serve as example if more tests are made.
	 * 
	 * @param gID
	 * @param gIDH
	 * @param oID
	 */
	public void setGID_AndLink_gross (String gID, String gIDH, Long oID) {
		boolean loaded_in_cache = this.isLoaded();
		
		// here is where the old GIDs are saved to verify if they are changed (which should not happen)
		// and to delete them from the loaded hashes.
		String oldGID = this.getGID();
		String oldGIDH = this.getGIDH();

		// sanitize input
		if (gID != null) {
			if (D_GIDH.isCompactedGID(gID)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gID = null;
			}
		}
		if (gIDH != null) {
			if (D_GIDH.isCompactedGID(gIDH)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gIDH = null;
			}
		}
		// sanitize GIDH
		if (gIDH != null) {
			if (! D_GIDH.isGIDH(gIDH)) {
				Util.printCallPath("Should be a GIDH: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gIDH = null;
			}
		}
		// sanitize input: infer a gidh if possible from GID
		if ((gID != null) && (gIDH == null)) {
			if (D_GIDH.isGID(gID)) {
				gIDH = D_Constituent.getGIDHashFromGID(gID);
			} else {
				if (D_GIDH.isGIDH(gID)) {
					gIDH = gID;
				} else {
					Util.printCallPath("Why? GID is nothing: gID="+gID+" IDH="+gIDH);
				}
				//gID = null; // done later
			}
			if (gIDH == null) Util.printCallPath("D_Constituent: null GIDH when setGID:"+gID+" for: "+this);
		}
		// sanitize  remove non gid
		if (gID != null) {
			if (! D_GIDH.isGID(gID)) {
//				if (!this.isExternal())
				Util.printCallPath("Should be a GID: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gID = null;
			}
		}
		
		if ( (gID != null) && ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (DEBUG && oldGID != null) Util.printCallPath("Why: new GID="+gID+" vs oldGID="+oldGID);
			if (oldGID != null) {
				if (D_GIDH.isGID(oldGIDH) && !D_GIDH.isGID(gID)) {
					// dead code: should never be here since we tested before
					gID = oldGIDH;
				} else {
					D_Constituent_Node.remByGID(oldGID, oID); //.loaded_const_By_GID.remove(oldGID);
					this._set_GID(gID);
					this.dirty_main = true;
				}
			} else {
				this._set_GID(gID);
				this.dirty_main = true;
			}
		}
		
//		// redundant sanitizing handling the GIDH
//		if (gIDH != null) {
//			if (! D_GIDH.isGIDH(gIDH)) {
//				Util.printCallPath("Why is this not a GIDH? GID="+gID+" IDH="+gIDH);
//				if (gID != null) gIDH = D_Constituent.getGIDHashFromGID(gID);
//				else gIDH = null;
//			}
//		}
		
		// handle GIDH
		if (gIDH != null) {
			if ( ! Util.equalStrings_null_or_not(oldGIDH, gIDH)) {		
				if (oldGIDH != null) {
					D_Constituent_Node.remByGIDH(this.getGIDH(), oID); //.loaded_const_By_GIDhash.remove(this.getGIDH());
					if (DEBUG) Util.printCallPath("Why? change to ID="+gID+" IDH="+gIDH);
				}
				this.setGIDH(gIDH);
				this.dirty_main = true;
			}
		}		
		
		if (loaded_in_cache) {
			if (this.getGID() != null)
				D_Constituent_Node.putByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
			if (this.getGIDH() != null)
				D_Constituent_Node.putByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
		}
	}

	public static boolean is_crt_const(D_Constituent candidate) {
		//D_Peer myself = data.HandlingMyself_Peer.get_myself();
		//if (myself == candidate) return true;
		return Application_GUI.is_crt_const(candidate);
	}
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	public void setSK(SK sk) {
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
		//this.testedSK = true;
	}
	/**
	 * Set to show that this was tested (unsuccessfully?) for existence of the sk in the database
	 */
	public boolean testedSK = false;
	/**
	 * Gets the SK from cache or DB.
	 * Assumes cache loaded (should be loaded when importing keys)!
	 * @return
	 */
	public SK getSK() {
		SK sk = null;
		if (keys != null) sk = keys.getSK();
		if (sk != null) return sk;
		if (testedSK) return null;

		PK pk = null;
		if (keys != null) pk = keys.getPK();
		
		String key_gID;
		
		if (! this.isExternal()) {
			key_gID = this.getGID();
			if (key_gID == null) {
				this.fillGlobals();
				key_gID = this.getGID();
			}
		} else {
			key_gID = this.getSubmitterGID();
			if (key_gID == null) {
				this.fillGlobals();
				key_gID = this.getSubmitterGID();
			}
		}

		if (key_gID == null) return null;
		/*
		if (keys != null) {
			//System.out.println("D_Peer:getSK: has to load SK keys when importing keys");
			//Util.printCallPath(""+this);
			return null; // this under assumption no sk if keys seen
		}
		*/
		
		sk = Util.getStoredSK(key_gID, this.getGIDH());
		this.testedSK = true;
		if (sk == null) return null;
		keys = Cipher.getCipher(sk, pk);
		//if (keys == null) return sk;
		return sk;
	}
	public String getSubmitterGID() {
		if (this.global_submitter_id != null) return this.global_submitter_id;
		if (this.getSubmitterLID() > 0) {
			this.global_submitter_id = D_Constituent.getGIDFromLID(this.getSubmitterLID());
			if (this.global_submitter_id != null) return this.global_submitter_id;
		}
		return null;
	}
	public void initSK() {
		if (this.isExternal()) return;
		String gID = this.getGID();
		SK sk = Util.getStoredSK(gID, this.getGIDH());
		testedSK = true;
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (get_StatusLockWrite() <= 0) Util.printCallPath("Null reference already!");
		else dec_StatusLockWrite();
		//System.out.println("D_Constituent: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (get_StatusLockWrite() > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Constituent> _need_saving = new HashSet<D_Constituent>();
	private static HashSet<D_Constituent> _need_saving_obj = new HashSet<D_Constituent>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Constituent> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Constituent> node) {
		DDP2P_DoubleLinkedList_Node<D_Constituent> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Constituent: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Constituent> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Constituent: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	/*
	static boolean need_saving_contains(String GIDH) {
		return _need_saving.contains(GIDH);
	}
	static void need_saving_add(String GIDH, String instance){
		_need_saving.add(GIDH);
	}
	*/
	static void need_saving_remove(D_Constituent c) {
		if (DEBUG) System.out.println("D_Constituent:need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Constituent org) {
		if (DEBUG) System.out.println("D_Constituent:need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Constituent need_saving_next() {
		Iterator<D_Constituent> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Constituent c = i.next();
		if (DEBUG) System.out.println("D_Constituent: need_saving_next: next: "+c);
		//D_Constituent r = D_Constituent_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Constituent Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Constituent Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Constituent need_saving_obj_next() {
		Iterator<D_Constituent> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Constituent r = i.next();
		if (DEBUG) System.out.println("D_Constituent: need_saving_obj_next: next: "+r);
		//D_Constituent r = D_Constituent_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Constituent Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Constituent Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Constituent> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Constituent i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Constituent_Node.loaded_objects.toString();
		s += "]";
		return s;
	}
	
	
	
	/**
	Sign_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * SIGN(C',<C,O,i,r>)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getSignableEncoder() {//String global_organization_id
		//this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		if(version>=2) enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(getOrganizationGID(),Encoder.TAG_PrintableString));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getSurname()!=null) enc.addToSequence(new Encoder(getSurname(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(getForename()!=null) enc.addToSequence(new Encoder(getForename(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(getEmail()!=null) enc.addToSequence(new Encoder(getEmail(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.getNeighborhoodGID()!=null)
			enc.addToSequence(new Encoder(this.getNeighborhoodGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(getPicture()!=null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC6));
		if(version<2) if(getHash_alg()!=null) enc.addToSequence(new Encoder(getHash_alg(),false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(getCertificate()!=null) enc.addToSequence(new Encoder(getCertificate()).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if(version<2) if(getSubmitterGID()!=null)enc.addToSequence(new Encoder(getSubmitterGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		if(getSlogan()!=null) enc.addToSequence(new Encoder(getSlogan()).setASN1Type(DD.TAG_AC13));
		if(getWeight()!=null) enc.addToSequence(new Encoder(getWeight()).setASN1Type(DD.TAG_AC14));
		enc.addToSequence(new Encoder(isExternal()));
		if(!isExternal() && version>=2) enc.addToSequence(new Encoder(isRevoked()));
		return enc;
	}
	/**
	HashExtern_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		-- version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		-- global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		-- creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		-- global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		-- slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		-- revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * Make GID for external: HASH(O,i)
	 * Without current GID, date, and slogan (externals should not have slogans?)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getHashEncoder(){//String global_organization_id) {
		//this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		
		enc.addToSequence(new Encoder(getOrganizationGID(),Encoder.TAG_PrintableString));
		//if(global_constituent_id!=null)enc.addToSequence(new Encoder(global_constituent_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getSurname()!=null) enc.addToSequence(new Encoder(getSurname(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(getForename()!=null) enc.addToSequence(new Encoder(getForename(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(getEmail()!=null)enc.addToSequence(new Encoder(getEmail(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.getNeighborhoodGID()!=null)
			enc.addToSequence(new Encoder(this.getNeighborhoodGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(getPicture()!=null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC6));
		if(version<2)if(getHash_alg()!=null)enc.addToSequence(new Encoder(getHash_alg(),false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(getCertificate()!=null) enc.addToSequence(new Encoder(getCertificate()).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		//if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		//if(slogan!=null)enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(getWeight()!=null) enc.addToSequence(new Encoder(getWeight()).setASN1Type(DD.TAG_AC14)); // weight should be signed by authority/witnessed
		enc.addToSequence(new Encoder(isExternal()));
		return enc;
		//if(true)return enc;
	}
	/**
	D_Constituent ::= IMPLICIT [APPLICATION 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString OPTIONAL,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		valid_support [APPLICATION 16] D_Witness OPTIONAL,
		submitter [APPLICATION 17] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN
	}
	 */
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) { 
		return getEncoder(dictionary_GIDs, 0);
	}
	/**
	 * TODO: Have to decide later if neighborhoods and submitter should be send even when descendants is none...
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		
		Encoder enc = new Encoder().initSequence();
		if (version >= 2) enc.addToSequence(new Encoder(version));
		
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (getOrganizationGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrganizationGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		}
		if (getGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		}
		if (getSurname()!=null) enc.addToSequence(new Encoder(getSurname(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if (getForename()!=null) enc.addToSequence(new Encoder(getForename(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if (address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if (getEmail()!=null) enc.addToSequence(new Encoder(getEmail(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if (creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if (getNeighborhoodGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getNeighborhoodGID());
			enc.addToSequence(new Encoder(repl_GID, false).setASN1Type(DD.TAG_AC10));
		}
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (getNeighborhood() != null) enc.addToSequence(Encoder.getEncoder(getNeighborhood(), dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC5));
		}
		
		if (getPicture() != null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC6));
		if (getHash_alg() != null)enc.addToSequence(new Encoder(getHash_alg(),false));
		if (getSignature() != null)enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC7));
		if (false && getGIDH() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGIDH());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		}
		if (getCertificate()!=null) enc.addToSequence(new Encoder(getCertificate()).setASN1Type(DD.TAG_AC9));
		if (languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if (getSubmitterGID()!=null){
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getSubmitterGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		}
		if (getSlogan() != null) enc.addToSequence(new Encoder(getSlogan()).setASN1Type(DD.TAG_AC13));
		if (getWeight() != null) enc.addToSequence(new Encoder(getWeight()).setASN1Type(DD.TAG_AC14));
		if (getValid_support() != null) enc.addToSequence(getValid_support().getEncoder().setASN1Type(DD.TAG_AC16));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (getSubmitter() != null) enc.addToSequence(getSubmitter().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC17));
		}
		enc.addToSequence(new Encoder(isExternal()));
		if (! isExternal()) enc.addToSequence(new Encoder(isRevoked()));
		enc.setASN1Type(D_Constituent.getASN1Type());
		return enc;
	}

	@Override
	public D_Constituent decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==Encoder.TAG_INTEGER) version = dec.getFirstObject(true).getInteger().intValue();
		else version = 0;
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)setOrganizationGID(dec.getFirstObject(true).getString());
		if(dec.getTypeByte()==DD.TAG_AC0) _set_GID(dec.getFirstObject(true).getString(DD.TAG_AC0));
		if(dec.getTypeByte()==DD.TAG_AC1) setSurname(dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC15)setForename(dec.getFirstObject(true).getString(DD.TAG_AC15));
		if(dec.getTypeByte()==DD.TAG_AC2) address = dec.getFirstObject(true).getSequenceOf(D_FieldValue.getASN1Type(),new D_FieldValue[]{},new D_FieldValue());
		if(dec.getTypeByte()==DD.TAG_AC3) setEmail(dec.getFirstObject(true).getString(DD.TAG_AC3));
		if(dec.getTypeByte()==DD.TAG_AC4) setCreationDate(dec.getFirstObject(true).getGeneralizedTime(DD.TAG_AC4));
		if(dec.getTypeByte()==DD.TAG_AC10)setNeighborhoodGID(dec.getFirstObject(true).getString(DD.TAG_AC10));
		if(dec.getTypeByte()==DD.TAG_AC5) setNeighborhood(dec.getFirstObject(true).getSequenceOf(D_Neighborhood.getASN1Type(),new D_Neighborhood[]{}, D_Neighborhood.getEmpty()));
		if(dec.getTypeByte()==DD.TAG_AC6) setPicture(dec.getFirstObject(true).getBytes(DD.TAG_AC6));
		if(dec.getTypeByte()==Encoder.TAG_PrintableString) setHash_alg(dec.getFirstObject(true).getString(Encoder.TAG_PrintableString));
		if(dec.getTypeByte()==DD.TAG_AC7)setSignature(dec.getFirstObject(true).getBytes(DD.TAG_AC7));
		if(dec.getTypeByte()==DD.TAG_AC8)setGIDH(dec.getFirstObject(true).getString(DD.TAG_AC8));
		if(dec.getTypeByte()==DD.TAG_AC9)setCertificate(dec.getFirstObject(true).getBytes(DD.TAG_AC9));
		if(dec.getTypeByte()==DD.TAG_AC11) languages = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC12) setSubmitterGID(dec.getFirstObject(true).getString(DD.TAG_AC12));
		if(dec.getTypeByte()==DD.TAG_AC13) setSlogan(dec.getFirstObject(true).getString(DD.TAG_AC13));
		if(dec.getTypeByte()==DD.TAG_AC14) setWeight(dec.getFirstObject(true).getString(DD.TAG_AC14));
		if(dec.getTypeByte()==DD.TAG_AC16) setValid_support(new D_Witness().decode(dec.getFirstObject(true)));
		if(dec.getTypeByte()==DD.TAG_AC17) setSubmitter(new D_Constituent().decode(dec.getFirstObject(true)));
		if(dec.getFirstObject(false) == null) throw new net.ddp2p.ASN1.ASNLenRuntimeException("No external found");
		setExternal(dec.getFirstObject(true).getBoolean());
		if(!isExternal())
			if(dec.getTypeByte()==Encoder.TAG_BOOLEAN)
				setRevoked(dec.getFirstObject(true).getBoolean());
		try{
			getGIDHashFromGID(false);
		}catch(Exception e){setGIDH(null);}
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.buildASN1byteType(Encoder.CLASS_APPLICATION,
				Encoder.PC_CONSTRUCTED, TAG);
	}
	public void setCreationDate() {
		this.setCreationDate(Util.CalendargetInstance());
	}
	public void setCreationDate(Calendar c) {
		this.creation_date = c;
		this._creation_date = Encoder.getGeneralizedTime(c);
		this.dirty_main = true;
	}
	public void setCreationDate(String creationDate) {
		this._creation_date = creationDate;
		this.creation_date = Util.getCalendar(creationDate);
		this.dirty_main = true;
	}
	public String getCreationDateStr() {
		return this._creation_date;
	}
	public Calendar getCreationDate() {
		return this.creation_date;
	}
	public void setArrivalDate(String _arrivalDate) {
		this._arrival_date = _arrivalDate;
		this.arrival_date = Util.getCalendar(_arrivalDate);
		this.dirty_locals = true;
	}
	public void setArrivalDate(Calendar arrivalDate) {
		this.arrival_date = arrivalDate;
		this._arrival_date = Encoder.getGeneralizedTime(arrivalDate);
		this.dirty_locals = true;
	}
	public void setArrivalDate() {
		this.setArrivalDate(Util.CalendargetInstance());
	}
	public String getArrivalDateStr() {
		return this._arrival_date;
	}
	public Calendar getArrivalDate() {
		return this.arrival_date;
	}
	public void setPreferencesDate(String preferencesDate) {
		this._preferences_date = preferencesDate;
		this.preferences_date = Util.getCalendar(preferencesDate);
		this.dirty_locals = true;
		//this.dirty_mydata = true;
	}
	public String getPreferencesDateStr() {
		return this._preferences_date;
	}
	public Calendar getPreferencesDate() {
		return this.preferences_date;
	}
	/**
	 * 
	 * @return
	 */
	public String getGIDHashFromGID(){
		return getGIDHashFromGID(true);
	}
	/**
	 * 
	 * @param verbose : false for silent on expected exception
	 * @return
	 */
	public String getGIDHashFromGID(boolean verbose){
		if (DEBUG) System.out.println("D_Constituent: prepareGIDHash: start");
		if ((getGIDH() == null) && (getGID() != null)) {
			if (isExternal()) setGIDH(getGID());
			else setGIDH(getGIDHashFromGID_NonExternalOnly(getGID(), verbose));
		}	
		if (DEBUG) System.out.println("D_Constituent: prepareGIDHash: got="+getGIDH());
		return getGIDH();
	}
	/**
	 * Adds "R:"/D_GIDH.d_ConsR in front of the hash of the GID, for non_external (verbose on error)
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID){
		return getGIDHashFromGID_NonExternalOnly(GID, true);
	}
	/**
	 *  Adds "R:" in front of the hash of the GID, for non_external
	 * @param GID
	 * @param verbose : false for silent on expected exceptions
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID, boolean verbose){
		String hash = Util.getGIDhashFromGID(GID, verbose);
		if (hash == null) return null;
		return D_GIDH.d_ConsR+hash;
	}
	/**
	 * unchanged if starts with "C:"/D_GIDH.d_ConsE (external) or "R:"/D_GIDH.d_ConsR (already a hash)
	 * @param s
	 * @return
	 */
	public static String getGIDHashFromGID(String s) {
		if (s.startsWith(D_GIDH.d_ConsE)) return s; // it is an external
		if (s.startsWith(D_GIDH.d_ConsR)) return s; // it is a GID hash
		String hash = D_Constituent.getGIDHashFromGID_NonExternalOnly(s);
		if (hash == null) return null;
		if (hash.length() != s.length()) return hash;
		return s;
	}
	/**
	 * If it is a GIDH (for a key), returns it. Otherwise returns null
	 * 
	 * @param s
	 * @return
	 */
	public static String getGIDFromGIDorGIDH(String s) {
		if (s.startsWith(D_GIDH.d_ConsE)) return s; // it is an external
		if (s.startsWith(D_GIDH.d_ConsR)) return s; //null; // it is a GIDhash
		
		String hash = D_Constituent.getGIDHashFromGID_NonExternalOnly(s);
		return hash;
//		if (hash == null) return null;
//		if (hash.length() != s.length()) return hash;
		//return null; //s;
	}
	public static boolean isGIDHash(String s) {
		if (s == null) return false;
		if (s.startsWith(D_GIDH.d_ConsE)) return true; // already hash
		if (s.startsWith(D_GIDH.d_ConsR)) return true; // grass root
		return false;
	}
	/**
	 * Should be called after the data is initialized.
	 * Does not assign the GID itself
	 * @return
	 */
	public String makeExternalGID() {
		fillGlobals();
		//return Util.getGlobalID("constituentID_neighbor",email+":"+forename+":"+surname);  
		return D_GIDH.d_ConsE+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	private void fillGlobals() {
		if (this.loaded_globals) return;
		
		if ((this.organization_ID != null ) && (this.getOrganizationGID() == null))
			this.setOrganizationGID(D_Organization.getGIDbyLID(this._organization_ID));
		
		this.loaded_globals = true;
	}
	/**
	 * Returns full name (surname, forname).
	 * Returns Surname Forename  (if LOCALIZATION_NAME =LOCALIZATION_NAME_EUROPE)
	 * Returns Forename Surname (if LOCALIZATION_NAME =LOCALIZATION_NAME_US_F_S)
	 * @return
	 */
	public String getNameFull() {
		if ((this.getForename()==null)||(this.getForename().trim().length()==0)) return getSurname();
		if ((this.getSurname()==null)||(this.getSurname().trim().length()==0)) return getSurname();

		if (LOCALIZATION_NAME == LOCALIZATION_NAME_EUROPE) return this.getSurname()+" "+this.getForename();
		if (LOCALIZATION_NAME == LOCALIZATION_NAME_US_F_S) return this.getForename()+" "+this.getSurname();
		return this.getSurname()+", "+ this.getForename();
	}
	/**
	 * If I did not set a pseudonym, returns the full name (surname, forename).
	 * @return
	 */
	public String getNameOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getNameFull();
	}
	public static String getGIDFromLID(String constituentlID) {
		return getGIDFromLID(Util.lval(constituentlID));
	}
	public static String getGIDFromLID(long constituentlID) {
		if (constituentlID <= 0) return null;
		Long LID = new Long(constituentlID);
		D_Constituent c = D_Constituent.getConstByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Constituent: getGIDFromLID: null for cLID = "+constituentlID);
			for (Long l : D_Constituent_Node.loaded_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Constituent: getGIDFromLID: available ["+l+"]"+D_Constituent_Node.loaded_By_LocalID.get(l).getGIDH());
			}
			return null;
		}
		return c.getGID();
	}
	/**
	 * Does not create but returns -1
	 * @param GID2
	 * @return
	 */
	public static long getLIDFromGID(String GID2, Long oID) {
		if (GID2 == null) return -1;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, null, true, false, oID);
		if (c == null) return -1;
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID) {
		if (GID2 == null) return null;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, null, true, false, oID);
		if (c == null) return null;
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return null;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, GIDH2, true, false, oID);
		if (c == null) return null;
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return -1;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, GIDH2, true, false, oID);
		if (c == null) return -1; else return c.getLID();
	}
	public static void setTemporary(D_Constituent c) {
		c = D_Constituent.getConstByConst_Keep(c);
		c.setTemporary();
		c.storeRequest();
		c.releaseReference();
	}
	/**
	 * Sets the temporary flag
	 */
	public void setTemporary() {
		this.setTemporary(true);
		//this.setSignature(null);
	}
	public void setTemporary(boolean b) {
		this.temporary = b;
		this.dirty_locals = true;
	}
	/**
	 * If marked temporary, no signature or no GID
	 * @return
	 */
	public boolean isTemporary() {
		return this.temporary 
				|| ( (! this.isExternal()) && (this.getSignature() == null || this.getSignature().length == 0) )
				|| this.getGID() == null;
	}
	
	public boolean readyToSend() {
		return this.isTemporary();
		/*
		if(this.global_constituent_id == null) return false;
		if(this.global_organization_ID == null) return false;
		if(!this.external)
			if((this.signature == null)||(this.signature.length==0)) return false;
		return true;
		*/
	}

	public static long insertTemporaryGID(String p_cGID,
			String p_cGIDH, long p_oLID, D_Peer __peer, boolean default_blocked) {
		D_Constituent consts = D_Constituent.insertTemporaryGID_org(p_cGID, p_cGIDH, p_oLID, __peer, default_blocked);
		if (consts == null) {
			Util.printCallPath("Why null");
			return -1;
		}
		return consts.getLID_force();//.getLID(); 
	}/** inserts temporary using also the organization LID */
	public static D_Constituent insertTemporaryGID_org(
			String p_cGID, String p_cGIDH, long p_oLID,
			D_Peer __peer, boolean default_blocked) {
		D_Constituent consts;
		if ((p_cGID != null) || (p_cGIDH != null)) {
			consts = D_Constituent.getConstByGID_or_GIDH(p_cGID, p_cGIDH, true, true, true, __peer, p_oLID);
			//consts.setName(_name);
			if (consts.isTemporary()) {
				consts.setBlocked(default_blocked);
				consts.storeRequest();
			}
			consts.releaseReference();
		}
		else return null;
		return consts; 
	}
	public void setHidden() {
		this.hidden = true;
		this.dirty_locals = true;
	}
	public boolean getHidden() {
		return this.hidden;
	}
	public void setBlocked(boolean default_blocked) {
		if (this.blocked != default_blocked) {
			this.dirty_locals = true;
			this.blocked = default_blocked;
		}
	}
	public void setOrganization(String goid, long added_Org) {
		if (DEBUG) System.out.println("D_Constituent: setOrganization: GID="+goid+" lID="+added_Org);
		if (goid == null) {
			goid = D_Organization.getGIDbyLID(added_Org);
			if (DEBUG) System.out.println("D_Constituent: setOrganization: recomputed GID="+goid);
		}
		if (! Util.equalStrings_null_or_not(goid, getOrganizationGID())) {
			if (DEBUG) System.out.println("D_Constituent: setOrganization: set goid old="+getOrganizationGID());
			if ((goid != null) && (getOrganizationGID() != null))
				if (_DEBUG) System.out.println("D_Constituent: setOrganization: set goid="+goid+" old="+getOrganizationGID());
			this.setOrganizationGID(goid);
			this.dirty_main = true;
		}
		if (_organization_ID != added_Org) {
			this._organization_ID = added_Org;
			this.organization_ID = Util.getStringID(_organization_ID);
			this.dirty_main = true;
		}
	}
	/**
	 * 
	 * @param GIDH
	 * @param DBG
	 * @return 0:absent, 1:available, -1: temporary
	 */
	public static int isGIDHash_available(String GIDH, Long oID,
			boolean DBG) {
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(null, GIDH, false, false, oID);
		if (c == null) return 0;
		if (c.isTemporary()) return -1;
		if (c.getGID() == null) return -1;
		
		if (! c.isExternal() && (c.getSignature() != null) ) {
			return 1;
		} else {
			return -1;
		}
	}
	/**
	 * 
	 * @param GIDH
	 * @param DBG
	 * @return 0:absent, 1:available, -1: temporary
	 */
	public static int isGID_available(String GID, Long oID,
			boolean DBG) {
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID, null, false, false, oID);
		if (c == null) return 0;
		if (c.isTemporary()) return -1;
		if (c.getGID() == null) return -1;
		
		if (! c.isExternal() && (c.getSignature() != null) ) {
			return 1;
		} else {
			return -1;
		}
	}
	public byte[] getSignature() {
		return this.signature;
	}
	public byte[] sign() {
		if (this.isExternal()) {
			if (this.getSubmitterGID() != null) {
				SK sk = getSK();
				if (sk != null)
					return sign_with_ini(sk);
			}
			return null;
		} else {
			SK sk = getSK();
			if (sk == null) return null;
			if(DEBUG) System.out.println("D_Constituent:sign: orgGID="+this.getOrganizationGID());
			byte[] msg=this.getSignableEncoder().getBytes();
			if(DEBUG) System.out.println("D_Constituent:sign: msg["+msg.length+"]="+Util.byteToHex(msg));
			return setSignature(Util.sign(msg, sk));
		}
	}
	public byte[] sign_with_ini(SK sk_ini) {
		D_Constituent wbc = this;
		String gcdhash, gcd;
		boolean present = isLoaded();
		if (wbc.isExternal()) {
			gcdhash = gcd = wbc.makeExternalGID();
			if (wbc.getGID() != null) {
				present |= (null != D_Constituent_Node.remByGID(wbc.getGID(), wbc.getOrganizationLID())); //.loaded_const_By_GID.remove(wbc.global_constituent_id));
			}
			if (wbc.getGIDH() != null) {
				present |= (null != D_Constituent_Node.remByGIDH(wbc.getGIDH(), wbc.getOrganizationLID()));//.loaded_const_By_GIDhash.remove(wbc.global_constituent_id_hash));
			}
//			wbc._set_GID(gcd);
//			wbc.global_constituent_id_hash = gcdhash;
//			if (present  || D_Constituent_Node.loaded_const_By_LocalID.get(this.getLID()) != null) {
//				D_Constituent_Node.putConstByGID(wbc.set_GID(gcd), wbc.getOrganizationID(), wbc); //.loaded_const_By_GID.put(wbc.global_constituent_id = gcd, wbc);
//				D_Constituent_Node.putConstByGIDH(wbc.global_constituent_id_hash = gcdhash, wbc.getOrganizationID(), wbc); //.loaded_const_By_GIDhash.put(wbc.global_constituent_id_hash = gcdhash, wbc);				
//			}
			wbc.setGID_AndLink(gcd, gcd, wbc.getOrganizationLID());
		}
		if ((sk_ini != null) || (! wbc.isExternal())) {
			byte[] msg = this.getSignableEncoder().getBytes();
			if(DEBUG) System.out.println("WB_Constituents:sign: msg["+msg.length+"]="+Util.byteToHex(msg));
			this.setSignature(Util.sign(msg, sk_ini));
		} else {
			setSignature(null);
		}
		return getSignature();
	}
	public boolean verifySignature() {
		String newGID;
		if(DEBUG) System.out.println("D_Constituents:verifySignature: orgGID="+getGID());
		this.fillGlobals();
		
		if (isExternal())
			if (!(newGID = this.makeExternalGID()).equals(this.getGID())){
				Util.printCallPath("WRONG EXTERNAL GID");
				if(DEBUG) System.out.println("D_Constituent:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
				if(DEBUG) System.out.println("D_Constituent:verifySignature: WRONG HASH GID result="+false);
				return false;
			}
		
		String pk_ID = this.getGID();
		if (isExternal()) {
			pk_ID = this.getSubmitterGID();
			if (pk_ID == null) return true;
		}
		if (DEBUG) System.out.println("D_Constituent:verifySignature: pk="+pk_ID);
		byte[] msg = getSignableEncoder().getBytes();
		boolean result = net.ddp2p.common.util.Util.verifySignByID(msg, pk_ID, getSignature());
		if (DEBUG) System.out.println("D_Constituents:verifySignature: result="+result);
		return result;
	}
	public static D_Constituent integrateRemote(D_Constituent c, D_Peer __peer,
			RequestData sol_rq, RequestData new_rq, boolean default_blocked, Calendar arrival_date) {
		long oID = D_Organization.getLIDbyGID (c.getOrganizationGID());
		if (oID <= 0) {
			D_Organization o = D_Organization.insertTemporaryGID_org(c.getOrgGID(), c.getOrgGIDH_if_available(), __peer, default_blocked, null);
			//c.setOrganization(o.getGID(), o.getLID());
			oID = o.getLID_forced();
		}
		D_Constituent lc =
				D_Constituent.getConstByGID_or_GIDH(c.getGID(), c.getGIDH(), true, true, true, __peer, oID);
		if (lc == null) return null;
		try{
			if (lc.loadRemote(sol_rq, new_rq, c, __peer, default_blocked, arrival_date)) {
				lc.storeRequest();
				net.ddp2p.common.config.Application_GUI.inform_arrival(lc, __peer);
			}
			lc.releaseReference();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lc;
	}
	/**
	 * Currently returns null
	 * @return
	 */
	public String getOrgGIDH_if_available() {
		return null;
	}
	public boolean loadRemote(RequestData sol_rq, RequestData new_rq, D_Constituent r, D_Peer __peer, boolean default_blocked) {
		return loadRemote(sol_rq, new_rq, r, __peer, default_blocked, Util.CalendargetInstance());
	}
	public boolean loadRemote(RequestData sol_rq, RequestData new_rq, D_Constituent remote, D_Peer __peer, boolean default_blocked, Calendar arrival_date) {
		boolean _t = true, _n = true; // n left false in temporary
		if (! (_t = this.isTemporary()) && ! (_n = newer(remote, this))) return false;
		
		if (DEBUG)
			System.out.println("D_Constituents: loadRemote: tmp t="+_t+" n="+_n+" old="+this.getCreationDateStr()+" remote="+remote.getCreationDateStr());
		
		this.version = remote.version;
		this.creation_date = remote.creation_date;
		this._creation_date = remote._creation_date;
		
		if (! Util.equalStrings_null_or_not(getOrganizationGID(), remote.getOrganizationGID())) {
			long oID = D_Organization.getLIDbyGID(remote.getOrganizationGID());
			if (this.getOrganizationGID() != null && oID <= 0) {
				D_Organization o = D_Organization.insertTemporaryGID_org(remote.getOrganizationGID(), null, __peer, default_blocked, null);
				this.setOrganization(o.getGID(), o.getLID_forced());
				if (new_rq != null && o.isTemporary()) new_rq.orgs.add(o.getGIDH_or_guess());
			} else
				this.setOrganization(remote.getOrganizationGID(), oID);
		}
		if (! Util.equalStrings_null_or_not(getGID(), remote.getGID())) {
			if (remote.getGID() != null) this.setGID_AndLink(remote.getGID(), remote.getGIDH(), this.getOrganizationLID());
			//if (r.global_constituent_id_hash != null) this.global_constituent_id_hash = r.global_constituent_id_hash;
			this.constituent_ID = null;
			this._constituent_ID = -1;
		}
		
		if (! Util.equalStrings_null_or_not(getSubmitterGID(), remote.getSubmitterGID())) {
			this.setSubmitterGID(remote.getSubmitterGID());
			long sID = D_Constituent.getLIDFromGID(remote.getSubmitterGID(), this._organization_ID);
			if (this.getSubmitterGID() != null && sID <= 0) {
				D_Constituent c = D_Constituent.insertTemporaryGID_org(remote.getSubmitterGID(), null, this.getOrganizationLID(), __peer, default_blocked);
				this.setSubmitter(c); //r.submitter;
				this.setSubmitter_ID(c.getLIDstr_force()); //r.submitter_ID;
				if (new_rq != null && c.isTemporary()) new_rq.cons.put(c.getGIDH(), DD.EMPTYDATE);
			}
		}
		if (! Util.equalStrings_null_or_not(this.getNeighborhoodGID(), remote.getNeighborhoodGID())) {
			this.setNeighborhoodGID(remote.getNeighborhoodGID());
			//this.neighborhood = null;// r.neighborhood;
			//this.neighborhood_ID = r.neighborhood_ID;
			D_Neighborhood n;
			long nID = D_Neighborhood.getLIDFromGID(this.getNeighborhoodGID(), this.getOrganizationLID());
			this.setNeighborhood_LID(Util.getStringID(nID));
			if (this.getNeighborhoodGID() != null && nID <= 0) {
				n = D_Neighborhood.getNeighByGID(this.getNeighborhoodGID(), true, true, false, __peer, this.getOrganizationLID());
				this.setNeighborhood_LID(Util.getStringID(n.getLID_force()));
				if (new_rq != null && n.isTemporary()) new_rq.neig.add(this.getNeighborhoodGID());
			}
			//this.setNeighborhoodIDs(global_neighborhood_ID, -1);
		}
		this.setSurname(remote.getSurname());
		this.setForename(remote.getForename());
		this.setEmail(remote.getEmail());
		this.setWeight(remote.getWeight());
		this.setRevoked(this.isRevoked() | remote.isRevoked());
		this.setSlogan(remote.getSlogan());
		this.setExternal(remote.isExternal());
		this.languages = remote.languages;
		this.setPicture(remote.getPicture());
		this.setHash_alg(remote.getHash_alg());
		this.setSignature(remote.getSignature());
		this.setCertificate(remote.getCertificate());
		//if (r.valid_support != null) this.valid_support = r.valid_support;
		//this.valid_support = null;
		
		this.dirty_main = true;
		if (net.ddp2p.common.data.D_FieldValue.different(this.address, remote.address)) {
			this.address = remote.address;
			this.dirty_params = true;
		}
		if ((sol_rq != null) && (sol_rq.cons != null)) sol_rq.cons.put(getGID(), DD.EMPTYDATE);
		this.dirty_main = true;
		if (this.source_peer_ID <= 0 && __peer != null)
			this.source_peer_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		this.setArrivalDate(arrival_date);
		return true;
	}
	String getLIDstr_force() {
		return Util.getStringID(this.getLID_force());
	}
	/**
	 * Is one newer than two?
	 * @param one
	 * @param two
	 * @return returns false if two is revoked, or one creation is newer
	 */
	public static boolean newer(D_Constituent one, D_Constituent two) {
		assert (one != null && two != null);
		if (two.isRevoked()) return false;
		return newer(one.getCreationDate(), two.getCreationDate());
	}
	/**
	 * If one newer than two?
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean newer(Calendar one, Calendar two) {
		if ((one != null) && (two == null)) return true;
		if ((one == null) && (two != null)) return false;
		if ((one == null) && (two == null)) return false;
		return one.compareTo(two) > 0;
	}
	public static boolean newer(String one, String two) {
		if ((one != null) && (two == null)) return true;
		if ((one == null) && (two != null)) return false;
		if ((one == null) && (two == null)) return false;
		return one.compareTo(two) > 0;
	}
	public static Hashtable<String, String> checkAvailability(
			Hashtable<String, String> cons, String orgID, boolean DBG) {
		if (DEBUG) System.out.println("D_Constituent: checkAvailability: start");
		Hashtable<String, String> result = new Hashtable<String, String>();
		for (String cHash : cons.keySet()) {
			if (DEBUG) System.out.println("D_Constituent: checkAvailability: cHash="+cHash);
			if (cHash == null) {
				if (_DEBUG) System.out.println("D_Constituent: checkAvailability: cHash was null");
				continue;
			}
			if ( ! available(cHash, cons.get(cHash), orgID, DBG)) {
				String cGIDHash = D_Constituent.getGIDFromGIDorGIDH(cHash);
				if (DEBUG) System.out.println("D_Constituent: checkAvailability: not available, obtained GIDH "+cGIDHash+" from "+cHash);
				if (cGIDHash != null) {
					if (DEBUG) System.out.println("D_Constituent: checkAvailability: not available "+cGIDHash);
					result.put(cGIDHash, DD.EMPTYDATE);
				} else {
					if (_DEBUG) System.out.println("D_Constituent: checkAvailability: not available null GIDH for: "+ cHash);
				}
			} else {
				if (DEBUG) System.out.println("D_Constituent: checkAvailability: available");
			}
		}
		return result;
	}
	/*
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : cons) {
			if(!available(cHash, orgID, DBG)) result.add(cHash);
		}
		return result;
	}
	*/
	/**
	 * 
	 * @param hash
	 * @param creation (not available if creation is newer than this)
	 * @param orgID
	 * @param DBG
	 * @return
	 */
	private static boolean available(String hash, String creation, String orgID, boolean DBG) {
		boolean result = true;
		
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH (null, hash, true, false, Util.Lval(orgID));
		if (
				(c == null) 
				|| (c.getOrganizationLID() != Util.lval(orgID)) 
				|| (D_Constituent.newer(creation, c.getCreationDateStr()))
				|| c.isTemporary()
				) result = false;
		
		if ((c != null) && c.isBlocked()) result = true;
		if (DEBUG || DBG) System.out.println("D_Constituent: available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	public boolean isBlocked() {
		return this.blocked;
	}
	public boolean toggleBlock() {
		this.blocked = ! this.blocked;
		this.dirty_locals = true;
		return this.blocked;
	}
	public boolean toggleBroadcast() {
		this.broadcasted = ! this.broadcasted;
		this.dirty_locals = true;
		return this.broadcasted;
	}
	public static void delConstituent(long constituentID) {
		boolean DEBUG = false;
		if(DEBUG) System.err.println("Deleting ID = "+constituentID);
		try{
			Application.getDB().delete(net.ddp2p.common.table.field_value.TNAME, new String[]{net.ddp2p.common.table.field_value.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.witness.TNAME, new String[]{net.ddp2p.common.table.witness.source_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.witness.TNAME, new String[]{net.ddp2p.common.table.witness.target_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.identity_ids.TNAME, new String[]{net.ddp2p.common.table.identity_ids.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.motion.TNAME, new String[]{net.ddp2p.common.table.motion.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.justification.TNAME, new String[]{net.ddp2p.common.table.justification.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.signature.TNAME, new String[]{net.ddp2p.common.table.signature.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.news.TNAME, new String[]{net.ddp2p.common.table.news.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.translation.TNAME, new String[]{net.ddp2p.common.table.translation.submitter_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.constituent.TNAME, new String[]{net.ddp2p.common.table.constituent.constituent_ID}, new String[]{constituentID+""});
			Application.getDB().delete(net.ddp2p.common.table.constituent.TNAME, new String[]{net.ddp2p.common.table.constituent.submitter_ID}, new String[]{constituentID+""});
			
			String cID = Util.getStringID(constituentID);
			unlinkMemory(cID);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Delete object and witnesses (but not its motions, etc)
	 * @param constituentID
	 * @return
	 */
	public static D_Constituent zapp(long constituentID) {
		String cID = Util.getStringID(constituentID);
		try {
			D_Constituent o = D_Constituent_Node.loaded_By_LocalID.get(constituentID);
			if (o != null) {
				if (o.get_StatusLockWrite() <= 0) {
					if (! D_Constituent_Node.dropLoaded(o, false)) {
						System.out.println("D_Constituent: deleteAllAboutOrg: referred = "+o.get_StatusLockWrite());
					}
				}
			}
			Application.getDB().delete(net.ddp2p.common.table.witness.TNAME,
					new String[]{net.ddp2p.common.table.witness.source_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.witness.TNAME,
					new String[]{net.ddp2p.common.table.witness.target_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
		
			//
			return deleteConsDescription(cID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	/**
	 * Delete just object and its memory copy (not witnesses and all)
	 * @param constituentID
	 * @return
	 */
	public static D_Constituent deleteConsDescription(String constituentID) {
		try {
			Application.getDB().delete(net.ddp2p.common.table.field_value.TNAME,
					new String[]{net.ddp2p.common.table.field_value.constituent_ID},
					new String[]{constituentID},
					DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.constituent.TNAME,
					new String[]{net.ddp2p.common.table.constituent.constituent_ID},
					new String[]{constituentID},
					DEBUG);
			return unlinkMemory(constituentID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	/**
	 * Forget object from memory
	 * @param cLID
	 * @return
	 */
	private static D_Constituent unlinkMemory(String cLID) {
		Long lID = new Long(cLID);
		D_Constituent constit = D_Constituent_Node.loaded_By_LocalID.get(lID);
		if (constit == null) { 
			System.out.println("D_Constituent: unlinkMemory: referred null const for: "+cLID);
			return null;
		}
		if (DEBUG) System.out.println("D_Constituent: unlinkMemory: dropped const");
		if (!D_Constituent_Node.dropLoaded(constit, true)) {
			if (DEBUG) System.out.println("D_Constituent: unlinkMemory: referred = "+constit.get_StatusLockWrite());
		} else {
			if (DEBUG) System.out.println("D_Constituent: unlinkMemory: no problem dropping");
		}
		return constit;
	}
	public static String readSignSave(long signedLID, long signerLID) {
		D_Constituent signed = D_Constituent.getConstByLID(signedLID, true, true);
		if (signed == null) {
			if (_DEBUG) System.out.println("D_Constituent: readSignSave: null for signedLID = "+signedLID+" signedLID="+signerLID);
			return null;
		}
		signed.setCreationDate();
		SK sk_ini = null;
		if (signerLID > 0) {
			D_Constituent signer = D_Constituent.getConstByLID(signerLID, true, false);
			sk_ini = signer.getSK();
		} else {
			if (! signed.isExternal())
				sk_ini = signed.getSK();
		}
		signed.sign_with_ini(sk_ini);
		
		signed.storeRequest();
		signed.releaseReference();
		return signed.getGID();
	}
/*
	public boolean isLoaded() {
		if (this.getLID() > 0) if (D_Constituent_Node.loaded_org_By_LocalID.get(this.getLID()) != null) return true;
		if (D_Constituent_Node.loaded_org_By_GIDhash.get(getGIDH()) != null) return true;
		if (D_Constituent_Node.loaded_org_By_GID.get(getGID()) != null) return true;
		return false;
	}
*/	
/*
	public boolean isLoadedInCache() {
		if (this.getLIDstr() != null)
			return ( null != D_Constituent_Node.loaded_const_By_LocalID.get(new Long(this.getLID())) );
	}
	*/
	public boolean isLoaded() {
		String GIDH, GID;
		if (!D_Constituent_Node.loaded_objects.inListProbably(this)) return false;
		long lID = this.getLID();
		long oID = this.getOrganizationLID();
		if (lID > 0)
			if ( null != D_Constituent_Node.loaded_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH()) != null)
			if ( null != D_Constituent_Node.getByGIDH(GIDH, oID) //.loaded_const_By_GIDhash.get(GIDH)
			) return true;
		if ((GID = this.getGID()) != null)
			if ( null != D_Constituent_Node.getConstByGID(GID, oID)  //.loaded_const_By_GID.get(GID)
			) return true;
		return false;
	}
	public byte[] setSignature(byte[] sign) {
		this.signature = sign;
		this.dirty_main = true;
		return sign;
	}
	/**
	 * If any is not empty, this procedure tries to set the other.
	 * @param nGID
	 * @param nLID
	 */
	public void setNeighborhoodIDs(String nGID, long nLID) {
		if (nGID == null && nLID > 0) {
			nGID = D_Neighborhood.getGIDFromLID(nLID);
		}
		if (nGID != null && nLID <= 0) {
			nLID = D_Neighborhood.getLIDFromGID(nGID, this.getOrganizationLID());
		}
		this.setNeighborhood_LID(Util.getStringID(nLID));
		this.setNeighborhoodGID(nGID);
		this.setNeighborhood(null);
		this.dirty_main = true;
	}
	public String getForename() {
		return this.forename;
	}
	public D_Organization getOrganization() {
		long oID = this.getOrganizationLID();
		if (oID <= 0) return null;
		return D_Organization.getOrgByLID_NoKeep(oID, true);
	}
	public static ArrayList<D_Constituent> getAllConstsByGID(String GID) {
		String sql =
				"SELECT "+" c."+net.ddp2p.common.table.constituent.constituent_ID+
						//" c."+table.constituent.name+
						//",c."+table.constituent.forename+
						//",o."+table.organization.name+
						" FROM "+net.ddp2p.common.table.constituent.TNAME+" AS c"+
						//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+") "+
						" WHERE c."+net.ddp2p.common.table.constituent.global_constituent_ID+"=?;"
						;
		ArrayList<D_Constituent> list = new ArrayList<D_Constituent>();
		try {
			ArrayList<ArrayList<Object>> r = Application.getDB().select(sql, new String[]{GID}, DEBUG);
			for (ArrayList<Object> o : r) {
				long lID = Util.lval(o.get(0));
				D_Constituent cons = D_Constituent.getConstByLID(GID, true, false);
				list.add(cons);
			}
			return list; //return new JComboBox<Object>(list.toArray());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return list;
	}
	public String getOrgGID() {
		return this.getOrganizationGID();
	}
	public static int getConstNBinNeighborhood(long n_ID) {
		boolean DEBUG= false;
		String sql_c =
				"SELECT COUNT(*) "+//table.constituent.neighborhood_ID+
				" FROM "+net.ddp2p.common.table.constituent.TNAME+
				" WHERE "+net.ddp2p.common.table.constituent.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.getDB().select(sql_c, new String[]{n_ID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return 0;
		}
		return Util.ival(c.get(0).get(0), 0);
			//return c.size();
	}
	public static ArrayList<Long> getConstInNeighborhood(long n_ID, long o_ID) {
		ArrayList<Long> sel_c = new ArrayList<Long>();
		String sql_c = 
				"select "+net.ddp2p.common.table.constituent.constituent_ID+
				" from "+net.ddp2p.common.table.constituent.TNAME+
				" where "+net.ddp2p.common.table.constituent.neighborhood_ID+"=? AND "+net.ddp2p.common.table.constituent.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.getDB().select(sql_c, new String[]{n_ID+"", o_ID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return sel_c;
		}
		for (int k = 0; k < c.size(); k++ ) {
			sel_c.add(Util.Lval(c.get(k).get(0)));
		}
		return sel_c;
	}
	public final static String constituents_by_neigh_values_sql =
			"SELECT "
	+ "fv." + net.ddp2p.common.table.field_value.value
	+ ", fv."+net.ddp2p.common.table.field_value.field_extra_ID
	+ ", COUNT(*)"
	+ ", fe." + net.ddp2p.common.table.field_extra.tip
	+ ", fe." + net.ddp2p.common.table.field_extra.partNeigh
	+ ", fv." + net.ddp2p.common.table.field_value.fieldID_above
	+ ", fv." + net.ddp2p.common.table.field_value.field_default_next
	+ ", fv." + net.ddp2p.common.table.field_value.neighborhood_ID
	+ " FROM "+net.ddp2p.common.table.field_value.TNAME+" AS fv " +
	// " JOIN field_extra ON fv.fieldID = field_extra.field_extra_ID " +
	" JOIN " + net.ddp2p.common.table.constituent.TNAME+" AS c ON c."+net.ddp2p.common.table.constituent.constituent_ID+" = fv."+net.ddp2p.common.table.field_value.constituent_ID +
	" JOIN " + net.ddp2p.common.table.field_extra.TNAME+" AS fe ON fe."+net.ddp2p.common.table.field_extra.field_extra_ID+" = fv."+net.ddp2p.common.table.field_value.field_extra_ID+
	" WHERE c." + net.ddp2p.common.table.constituent.organization_ID+"=? "
	  + " AND ("
	  + " (fv."+net.ddp2p.common.table.field_value.field_extra_ID+" = ?)"
	  + " OR (fv."+net.ddp2p.common.table.field_value.fieldID_above+" ISNULL AND fe."+net.ddp2p.common.table.field_extra.partNeigh+" > 0) "
	  + ") "
	+ " GROUP BY fv."+net.ddp2p.common.table.field_value.value
	+ " ORDER BY fv."+net.ddp2p.common.table.field_value.value+" DESC;";
	/**
	 * <fv.value,fv.fe_ID,count,fe.tip,fe.neigh,fv.fe_above,fv.fe_next,fv.neigh_ID> grouped by value
	 * @param fe_ID
	 * @param o_ID
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getRootConstValues(long fe_ID, long o_ID) {
		
		if (DEBUG) System.out.println("D_Constituent: getRootConstValues: "+constituents_by_neigh_values_sql+" // "+ o_ID+","+fe_ID);
		
		ArrayList<ArrayList<Object>> subneighborhoods = new ArrayList<ArrayList<Object>>();
		try {
			subneighborhoods = Application.getDB().select( constituents_by_neigh_values_sql,
				new String[]{Util.getStringID(o_ID),
				Util.getStringID(fe_ID)}, DEBUG);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subneighborhoods;
	}
	/**
	 * Compute how many of the fields are not neighborhood parts (i.e. <= 0)
	 * @return
	 */
	public int getFieldValuesFixedNB() {
		if (address == null) return 0;
		D_Organization org = D_Organization.getOrgByLID_NoKeep(_organization_ID, true);
		int result = 0;
		for ( int k = 0; k < address.length ; k ++ ) {
			D_OrgParam fe = address[k].field_extra;
			if (fe == null) address[k].field_extra = fe = org.getFieldExtra(address[k].field_extra_GID);
			if (fe == null) continue;
			if (fe.partNeigh <= 0) result ++;
		}
		return result;
	}
	public void setFieldValue(D_FieldValue fv) {
		// TODO Auto-generated method stub
		if (address == null) address = new D_FieldValue[0];
		D_FieldValue[] _address = new D_FieldValue[address.length + 1];
		for (int k = 0; k < _address.length - 1; k ++) {
			_address[k] = address[k];
		}
		_address[_address.length - 1] = fv;
		address = _address;
		this.dirty_params = true;
	}
	
	public D_FieldValue[] getFieldValues() {
		return address;
	}
	/**
	 * 
	 * @param field
	 * @return
	 */
	public D_FieldValue getFieldValue(D_OrgParam field) {
		return getFieldValue(getFieldValues(), field);
	}
	/**
	 * 
	 * @param field_values
	 * @param field
	 * @return
	 */
	public static D_FieldValue getFieldValue(D_FieldValue[] field_values, D_OrgParam field) {
		if (field_values == null) return null;
		if (field == null) return null;
		for (int k = 0; k < field_values.length; k ++) {
			if (field_values[k].field_extra_ID == field.field_LID) return field_values[k];
			if (Util.equalStrings_null_or_not(field_values[k].field_extra_GID, field.global_field_extra_ID))
				return field_values[k];
		}
		return null;
	}
	public String getSlogan() {
		return this.slogan;
	}
	public String getEmail() {
		return this.email;
	}
	public String getSubmitterLIDstr() {
		return this.getSubmitter_ID();
	}
	public static ArrayList<Long> getOrphans(long o_ID) {
		boolean DEBUG = false;
		ArrayList<Long> r = new ArrayList<Long>();
	    	String sql = 
	    		"SELECT "
				//+ " c."+table.constituent.name+
				//", c."+table.constituent.forename+
		        //","
		        + " c."+net.ddp2p.common.table.constituent.constituent_ID+
		        //", c."+table.constituent.external+
		        //", c."+table.constituent.global_constituent_ID +
		        //", c."+table.constituent.submitter_ID +
		        //", c."+table.constituent.slogan +
		        //", c."+table.constituent.email +
	    		//+table.constituent._fields_constituents+
	    		" FROM "+net.ddp2p.common.table.constituent.TNAME+" AS c "+
	    		" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(c."+net.ddp2p.common.table.constituent.neighborhood_ID+"=n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "+
	    		" WHERE ( c."+net.ddp2p.common.table.constituent.neighborhood_ID+" ISNULL OR n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+" ISNULL )" +
	    		((DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG)?" AND ( c."+net.ddp2p.common.table.constituent.organization_ID+"=? ) ":"")+
	    				";";
		
	    		String[] params = new String[]{};
	    		if(DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG) params = new String[]{Util.getStringID(o_ID)};
	    		try {
					ArrayList<ArrayList<Object>> identities = Application.getDB().select(sql, params, DEBUG);
					for (int k = 0; k < identities.size(); k ++) {
						r.add(Util.Lval(identities.get(k).get(0)));
					}
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
				return r;
	}
	public static final String sql_org_all_constituents = "SELECT count(*) FROM "+net.ddp2p.common.table.constituent.TNAME+
		" WHERE "+net.ddp2p.common.table.constituent.organization_ID+" = ? AND "+net.ddp2p.common.table.constituent.op+" = ?;";
	/**
	 * 
	 * @param orgID
	 * @return
	 */
	public static Object getConstNBinOrganization(String orgID) {
		Object result = null;
		try {
			ArrayList<ArrayList<Object>> orgs = Application.getDB().select(sql_org_all_constituents, new String[]{orgID, "1"});
			if(orgs.size()>0) result = orgs.get(0).get(0);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	public int get_StatusReferences() {
		return status_references;
	}
	public int inc_StatusReferences() {
		this.assertReferenced(); // keep it in the process to avoid it being dropped before inc
		Application_GUI.ThreadsAccounting_ping("Raised constituent references for "+this.getNameFull());
		return status_references++;
	}
	public void dec_StatusReferences() {
		if (this.get_StatusReferences() <= 0) {
			Util.printCallPath("Why getting: "+get_StatusReferences());
			return;
		}
		this.status_references--;
		Application_GUI.ThreadsAccounting_ping("Dropped constituent references for "+this.getNameFull());
	}
	public int get_StatusLockWrite() {
		return status_lock_write;
	}
	StackTraceElement[] lastPath;
	final private Object monitor_reserve = new Object();
	public void inc_StatusLockWrite() {
		if (this.get_StatusLockWrite() > 0) {
			//Util.printCallPath("Why getting: "+getStatusReferences());
			//Util.printCallPath("D_Peer: incStatusReferences: Will sleep for getting: "+getStatusLockWrite()+" for "+getName());
			//Util.printCallPath(lastPath, "Last lp path was: ", "     ");
			int limit = 1;
//			if (this == data.HandlingMyself_Peer.get_myself_or_null()) {
//				limit = 2;
//			}
			synchronized(monitor_reserve) {
				if (
						(this.get_StatusLockWrite() >= limit)
						||
						(this.get_StatusLockWrite() >= limit)
						)
					try {
						do {
							Application_GUI.ThreadsAccounting_ping("Wait peer references for "+getNameFull());
							monitor_reserve.wait(10000); // wait 5 seconds, and do not re-sleep on spurious wake-up
							Application_GUI.ThreadsAccounting_ping("Got peer references for "+getNameFull());
							//Util.printCallPath("D_Peer: incStatusReferences: After sleep is getting: "+getStatusLockWrite()+" for "+getName());
							if (this.get_StatusLockWrite() > limit) Util.printCallPath(lastPath, "Last l path was: ", "     ");
							if (DD.RELEASE) break;
						} while (this.get_StatusLockWrite() > limit);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if (this.get_StatusLockWrite() >= limit) {
					Util.printCallPath(this+"\nSpurious wake after 5s this=: "+get_StatusLockWrite()+" for "+getNameFull());
					Util.printCallPath(lastPath, "Last path was: ", "     ");
				}
			}
		}
		lastPath = Util.getCallPath();
		this.status_lock_write++;
	}
	public void dec_StatusLockWrite() {
		if (this.get_StatusLockWrite() <= 0) {
			Util.printCallPath("Why getting: "+get_StatusLockWrite());
			return;
		}
		this.status_lock_write--;
		// Application_GUI.ThreadsAccounting_ping("Drop peer references for "+getName());
		synchronized(monitor_reserve) {
			monitor_reserve.notify();
		}
		//Application_GUI.ThreadsAccounting_ping("Dropped peer references for "+getName());
	}
	public static final String sql_all_constituents =
			"SELECT " + net.ddp2p.common.table.constituent.constituent_ID +
			" FROM " + net.ddp2p.common.table.constituent.TNAME +
			" WHERE " + net.ddp2p.common.table.constituent.organization_ID +"=?;";
	public static final String sql_all_constituents_in_neigh =
			"SELECT " + net.ddp2p.common.table.constituent.constituent_ID + 
			" FROM " + net.ddp2p.common.table.constituent.TNAME +
			" WHERE " + net.ddp2p.common.table.constituent.organization_ID +"=? " +
					" AND " + net.ddp2p.common.table.constituent.neighborhood_ID + "=?;";
	public static final String sql_all_constituents_in_root =
			"SELECT " + net.ddp2p.common.table.constituent.constituent_ID + 
			" FROM " + net.ddp2p.common.table.constituent.TNAME +
			" WHERE " + net.ddp2p.common.table.constituent.organization_ID +"=? " +
					" AND ( " + net.ddp2p.common.table.constituent.neighborhood_ID + " IS NULL  " +
							" OR " + net.ddp2p.common.table.constituent.neighborhood_ID + "<= 0 ) ;";
	/**
	 * Get all constituents in a given neighborhood.
	 * @param o_LID
	 * @param n_LID
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getAllConstituents(long o_LID, long n_LID, boolean all) {
		try {
			if (all)
				return Application.getDB().select(sql_all_constituents,
						new String[]{Util.getStringID(o_LID)});
			else if (n_LID <= 0) {
				return Application.getDB().select(sql_all_constituents_in_root,
						new String[]{Util.getStringID(o_LID)});
			} else {
				return Application.getDB().select(sql_all_constituents_in_neigh,
						new String[]{Util.getStringID(o_LID), Util.getStringID(n_LID)});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<ArrayList<Object>>();
	}
	public boolean isRevoked() {
		return revoked;
	}
	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
		this.dirty_main = true;
	}
	public void setEmail(String email) {
		this.email = email;
		this.dirty_main = true;
	}
	public void setSlogan(String slogan) {
		this.slogan = slogan;
		this.dirty_main = true;
	}
	public void setForename(String forename) {
		this.forename = forename;
		this.dirty_main = true;
	}
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
		this.dirty_main = true;
	}
	public int getWeightInt() {
		return Util.ival(weight, 1);
	}
	public double getWeightDouble() {
		return Util.dval(weight, 1.);
	}
	public String getWeight() {
		return weight;
	}
	/**
	 * Not checking limits for organization
	 * @param weight
	 * @return
	 */
	public boolean setWeight(String weight) {
		this.weight = weight;
		this.dirty_main = true;
		return true;
	}
	/**
	 * Checks organization limits. Fails if no org set, or limits not respected.
	 * @param weight
	 * @return
	 */
	public boolean setWeight(int weight) {
		if (weight == 1) return this.setWeight(weight + "");
		D_Organization org = this.getOrganization();
		if (org == null) {
			if (_DEBUG) System.out.println("D_Constituent: setWeight: null organization");
			return false;
		}
		if (org.getWeightsMax() < weight) {
			if (_DEBUG) System.out.println("D_Constituent: setWeight: weight exeeds max:"+org.getWeightsMax()+" < "+ weight);
			return false;
		}
		if (! org.hasWeights() && (weight != 1)) {
			if (_DEBUG) System.out.println("D_Constituent: setWeight: weights not allowed here: "+weight);
			return false;
		}
		return this.setWeight(weight + "");
	}
	/**
	 * Sets 1 on true and 0 on false
	 * @param voter
	 * @return
	 */
	public boolean setWeight(boolean voter) {
		return setWeight(voter?1:0);
	}
	/**
	 * Sets an object with at least an image, a url, or a non-negative is
	 * @param _icon
	 * @return
	 */
	public boolean setIconObject(IconObject _icon) {
		if (_icon == null || _icon.empty()) {
			return setIcon(null);
		}
		return setIcon(_icon.encode());
	}
	public IconObject getIconObject() {
		IconObject ic = new IconObject();
		if (this.picture == null) return ic;
		try {
			Decoder dec = new Decoder(this.picture);
			if (dec.getTypeByte() == IconObject.getASN1Type()) {
				ic.decode(dec);
				if (! ic.empty()) {
					return ic;
				}
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		ic.setImage(this.picture);
		return ic;
	}
	public byte[] getIcon() {
		if (this.picture == null) return null;
		try {
			Decoder dec = new Decoder(this.picture);
			if (dec.getTypeByte() == IconObject.getASN1Type()) {
				IconObject ic = new IconObject().decode(dec);
				if (! ic.empty()) {
					return ic.getImage();
				}
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		return this.picture;
	}
	/**
	 * Returns false if the image is larger than DD.MAX_CONSTITUENT_ICON_LENGTH
	 * @param _icon
	 * @return
	 */
	public boolean setIcon(byte[] _icon) {
		if (_icon != null && _icon.length > DD.MAX_CONSTITUENT_ICON_LENGTH) return false;
		this.picture = _icon;
		this.dirty_main = true;
		return true;
	}
	/**
	 * This calls setIcon(picture)
	 * @param picture
	 * @return
	 */
	@Deprecated
	public boolean setPicture(byte[] picture) {
		return setIcon(picture);
	}
	/**
	 * Calls getIcon()
	 * @return
	 */
	@Deprecated
	public byte[] getPicture() {
		return getIcon();//return picture;
	}
	public String getHash_alg() {
		return hash_alg;
	}
	public void setHash_alg(String hash_alg) {
		this.hash_alg = hash_alg;
		this.dirty_main = true;
	}
	public byte[] getCertificate() {
		return certificate;
	}
	public void setCertificate(byte[] certificate) {
		this.certificate = certificate;
		this.dirty_main = true;
	}
	public D_Witness getValid_support() {
		return valid_support;
	}
	public void setValid_support(D_Witness valid_support) {
		this.valid_support = valid_support;
	}
	public D_Neighborhood[] getNeighborhood() {
		return neighborhood;
	}
	public void setNeighborhood(D_Neighborhood[] neighborhood) {
		this.neighborhood = neighborhood;
	}
	public D_Constituent getSubmitter() {
		return submitter;
	}
	/**
	 * Just sets the object
	 * @param submitter
	 */
	public void setSubmitter(D_Constituent submitter) {
		this.submitter = submitter;
	}
	public String getSubmitter_ID() {
		return submitter_ID;
	}
	public void setSubmitter_ID(String submitter_ID) {
		this.submitter_ID = submitter_ID;
	}
	public String getNeighborhood_LID() {
		return neighborhood_ID;
	}
	public void setNeighborhood_LID(String neighborhood_ID) {
		this.neighborhood_ID = neighborhood_ID;
		this.dirty_main = true;
	}
	public String getOrganizationGID() {
		return global_organization_ID;
	}
	public void setOrganizationGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
		this.dirty_main = true;
	}
	public String getNeighborhoodGID() {
		return global_neighborhood_ID;
	}
	public void setNeighborhoodGID(String global_neighborhood_ID) {
		this.global_neighborhood_ID = global_neighborhood_ID;
		this.dirty_main = true;
	}
	public void setSubmitterGID(String global_submitter_id) {
		this.global_submitter_id = global_submitter_id;
		this.dirty_main = true;
	}
	public String getGIDH() {
		return global_constituent_id_hash;
	}
	public void setGIDH(String global_constituent_id_hash) {
		this.global_constituent_id_hash = global_constituent_id_hash;
	}
	/**
	 * Checks that LID and GID are not null
	 * @return
	 */
	public boolean realized() {
		if (this.getGID() == null) return false;
		if (this.getLIDstr() == null) return false;
		return true;
	}
	public static int getNumberItemsNeedSaving() {
		return _need_saving.size() + _need_saving_obj.size();
	}
	public static void stopSaver() {
		saverThread.turnOff();
	}
}
class D_Constituent_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	boolean stop = false;
	//public static final long SAVER_SLEEP_CONSTITUENT_ON_ERROR = 2000; // this is the single one used of this type
	/**
	 * The next monitor is needed to ensure that two D_Constituent_SaverThreadWorker are not concurrently modifying the cache and database,
	 * and no thread is started when it is not needed (since one is already running).
	 * The database is also protected by a monitor in the object (around the storeAct).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	public void turnOff() {
		stop = true;
		this.interrupt();
	}
	D_Constituent_SaverThread() {
		super("D_Constituent Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			if (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverThreads() < SaverThreadsConstants.MAX_NUMBER_CONCURRENT_SAVING_THREADS && D_Constituent.getNumberItemsNeedSaving() > 0)
			synchronized(saver_thread_monitor) {
				new D_Constituent_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Constituent de = D_Constituent.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Constituent_Saver: loop saving "+de);
					ThreadsAccounting.ping("Saving");
					D_Peer.need_saving_remove(de.getGIDH(), de.instance);
					// try 3 times to save
					for (int k = 0; k < 3; k++) {
						try {
							de.storeAct();
							break;
						} catch (P2PDDSQLException e) {
							e.printStackTrace();
							synchronized(this){
								try {
									wait(SAVER_SLEEP_ON_ERROR);
								} catch (InterruptedException e2) {
									e2.printStackTrace();
								}
							}
						}
					}
				} else {
					ThreadsAccounting.ping("Nothing to do!");
					//System.out.println("D_Constituent_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					long timeout = (D_Constituent.getNumberItemsNeedSaving() > 0)?
							SaverThreadsConstants.SAVER_SLEEP_BEETWEEN_CONSTITUENT_MSEC:
								SaverThreadsConstants.SAVER_SLEEP_WAITING_CONSTITUENT_MSEC;
					wait(timeout);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
			}
		}
	}
}

class D_Constituent_SaverThreadWorker extends net.ddp2p.common.util.DDP2P_ServiceThread {
	boolean stop = false;
	//public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Constituent_SaverThreadWorker() {
		super("D_Constituent Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_cons ++;}
		try{__run();} catch (Exception e){}
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_cons --;}
	}
	@SuppressWarnings("unused")
	public void __run() {
		synchronized(D_Constituent_SaverThread.saver_thread_monitor) {
			D_Constituent de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Constituent_Saver: start");
			
			 // first try objects being edited
			de = D_Constituent.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Constituent.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Constituent_Saver: loop saving "+de.getNameFull());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Constituent.need_saving_obj_remove(de);
				else D_Constituent.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Constituent_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < net.ddp2p.common.data.SaverThreadsConstants.ATTEMPTS_ON_ERROR; k++) {
					try {
						if (DEBUG) System.out.println("D_Constituent_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Constituent_Saver: stored org:"+de.getNameFull());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Constituent_Saver: sleep");
								wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_CONSTITUENT_ON_ERROR);
								if (DEBUG) System.out.println("D_Constituent_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Constituent_Saver: idle ...");
			}
		}
		if (SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_CONSTITUENT_MSEC >= 0) {
			synchronized(this) {
				try {
					if (DEBUG) System.out.println("D_Constituent_Saver: sleep");
					wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_CONSTITUENT_MSEC);
					if (DEBUG) System.out.println("D_Constituent_Saver: waked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

