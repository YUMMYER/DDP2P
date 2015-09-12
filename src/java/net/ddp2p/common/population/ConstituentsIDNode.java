package net.ddp2p.common.population;

import static net.ddp2p.common.util.Util.__;

import java.util.ArrayList;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_OID;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.util.Util;

public class ConstituentsIDNode extends ConstituentsBranch {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private int fixed_fields = 1;
	private ConstituentData constituent;
	String sql;
	String sql_params[] = new String[0];
	public void updateWitness(){  
		ArrayList<ArrayList<Object>> identities;
        try{
                String sql = "select w."+net.ddp2p.common.table.witness.sense_y_n+", count(*) " +
                                " from "+net.ddp2p.common.table.witness.TNAME+" as w " +
                                " where w."+net.ddp2p.common.table.witness.target_ID+" = ? " +
                                " GROUP BY w."+net.ddp2p.common.table.witness.sense_y_n+"; ";
                identities = Application.db.select(sql, new String[]{getConstituent().getC_LID()+""});
                getConstituent().witness_against = getConstituent().witness_for = 0;
                for (int k = 0; k < identities.size(); k ++) {
                        int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                        int wcount = Util.ival(identities.get(k).get(1), 1);
                        if (wsense == D_Witness.FAVORABLE) getConstituent().witness_for = wcount;
                        else if(wsense==D_Witness.UNFAVORABLE) getConstituent().witness_against = wcount;
                        else getConstituent().witness_neuter = wcount;
                }
                
                sql = "select w."+net.ddp2p.common.table.witness.sense_y_n +
                " from "+net.ddp2p.common.table.witness.TNAME+" as w " +
                " where w."+net.ddp2p.common.table.witness.target_ID+" = ? and w."+net.ddp2p.common.table.witness.source_ID+" = ? " +
                " ; ";
                long myself = model.getConstituentIDMyself();
                identities = Application.db.select(sql, 
                		new String[]{getConstituent().getC_LID()+"", myself+""});
                getConstituent().witnessed_by_me=D_Witness.UNSPECIFIED;
                if (identities.size() > 1) Application_GUI.warning(__("For constituent:")+" "+getConstituent().getC_LID(), __("Multiple witness from me"));
                for(int k=0; k<identities.size(); k++) {
                	int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                	getConstituent().witnessed_by_me = wsense;
                	if(DEBUG) System.err.println("Witnessed by me with: "+wsense);
                }
                if(myself == getConstituent().getC_LID()) getConstituent().myself = 1; 
                else getConstituent().myself = 0; 
        }catch(Exception e){
                //JOptionPane.showMessageDialog(null,"populate witnesses: "+e.toString());
        		//Application_GUI.warning("populate witnesses: "+e.toString(), "Issue Populating Witness");
                e.printStackTrace();                    
        }
	}
	public ConstituentsIDNode (ConstituentsInterfaceInput _model, ConstituentsBranch _parent, ConstituentData _data,
			String _sql_children, String[] _sql_params,
			Constituents_AddressAncestors[] _ancestors) {
		super(_model, _parent, _ancestors, 0);
		sql = _sql_children;
		sql_params = _sql_params;
		setConstituent(_data);
		
		D_Constituent cons = D_Constituent.getConstByLID(getConstituent().getC_LID(), true, false);
		if (cons == null) return;
		if(_data.external)  fixed_fields ++;
		setNchildren(cons.getFieldValuesFixedNB() + fixed_fields);
		
//		ArrayList<ArrayList<Object>> identities;
//		String params[] = new String[]{""+constituent.constituentID};
//		try {
//			String sql = "select count(*) from "+table.field_value.TNAME+" AS fv " +
//					" JOIN "+table.constituent.TNAME+" as c ON c."+table.constituent.constituent_ID+"=fv."+table.field_value.constituent_ID +
//					" JOIN "+table.field_extra.TNAME+" as fe ON fv."+table.field_value.field_extra_ID+"=fe."+table.field_extra.field_extra_ID +
//					" where fe."+table.field_extra.partNeigh+" <= 0 AND fv."+table.field_value.constituent_ID+" = ?;";
//			identities = model.db.select(sql, params, DEBUG);
//		}catch(Exception e) {
//			JOptionPane.showMessageDialog(null,"populate: "+e.toString());
//    		e.printStackTrace();
//    		return;
//		}
//		nchildren = Integer.parseInt(identities.get(0).get(0).toString()) + fixed_fields;

		updateWitness();
		if(DEBUG) System.err.println("ConstituentsModel: ConstituentsIDNode: "+getConstituent()+" #"+getNchildren());
	}
    public int getIndexOfChild(Object child) {
     	if((child==null)||!(child instanceof ConstituentsPropertyNode)) return -1;
    	for(int i=0; i<getChildren().length; i++){
    		if (getChildren()[i] == child) return i;
    		if (((ConstituentsPropertyNode)getChildren()[i]).get_field_valuesID()==
    			((ConstituentsPropertyNode)child).get_field_valuesID()) return i;
    	}
    	return -1;
    }
    public long get_constituentID() {
    	if(getConstituent() == null) return -1;
    	return getConstituent().getC_LID();
    }
    public String getTip() {
		if ((getConstituent() == null)||(getConstituent().getSlogan() == null)) return null;
    	return getConstituent().getSlogan();
    }
    /**
     * The display is based on ConstituentTree:getTreeCellRendererComponentCIN
     */
    public String toString() {
    	String result;
		if(getConstituent() == null){
			if(DEBUG) System.err.println("ConstituentsIDNodeModel: toString null const");
			return __("Unknown!");
		}
		if ((getConstituent().given_name == null) || 
				(getConstituent().given_name.equals("")))
			result = getConstituent().surname;
		else result = getConstituent().surname+", "+getConstituent().given_name;    
		return result+ " "+getConstituent().email+ " ::"+ getConstituent().getSlogan();
    }
    public void populate() {
    	//boolean DEBUG = true;
    	if (DEBUG) System.err.println("ConstituentsIDNode: populate this="+this);
		setChildren(new ConstituentsPropertyNode[0]);
		D_Constituent c = D_Constituent.getConstByLID(getConstituent().getC_LID(), true, false);
		if (c == null) { // || c.address == null) {
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address for: "+getConstituent().getC_LID());
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address: "+c);
			//return;
		}
		
//		ArrayList<ArrayList<Object>> identities;
//		String params[] = new String[]{""+constituent.constituentID};
//		try {
//			String sql =
//				"SELECT fv."+table.field_value.value + ", fe."+table.field_extra.label+
//				", o."+table.oid.OID_name+", o."+table.oid.sequence+", o."+table.oid.explanation+
//					" FROM "+table.field_value.TNAME+" AS fv " +
//					" JOIN "+table.constituent.TNAME+" AS c ON c."+table.constituent.constituent_ID+"=fv."+table.field_value.constituent_ID +
//					" JOIN "+table.field_extra.TNAME+" AS fe ON fv."+table.field_value.field_extra_ID+"=fe."+table.field_extra.field_extra_ID +
//					" LEFT JOIN "+table.oid.TNAME+" AS o ON o."+table.oid.sequence+"=fe."+table.field_extra.oid +
//					" WHERE ( fe."+table.field_extra.partNeigh+" <= 0 OR fe."+table.field_extra.partNeigh+" IS NULL ) AND fv."+table.constituent.constituent_ID+" = ?;";
//			identities = model.db.select(sql, params, DEBUG);
//		}catch(Exception e) {
//			JOptionPane.showMessageDialog(null,"ConstituentsIDNode:populate: "+e.toString());
//    		e.printStackTrace();
//    		return;
//		}
		int identities_size = 0;
		D_Organization org = null;
		
		org = D_Organization.getOrgByLID_NoKeep(c.getOrganizationLID(), true);
		
		if (c.address != null) {
			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+c.address.length);
			for ( int i = 0 ; i < c.address.length; i ++ ) {
				if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+i+" -> "+c.address[i]);
	    		
	    		D_OrgParam fe = c.address[i].field_extra;
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 1 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			fe = org.getFieldExtra(c.address[i].field_extra_GID);
	    		}
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 2 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			continue;
	    		}
	    		if (fe.partNeigh > 0) {
	    			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses 3 neigh fe #"+i+" - "+fe.partNeigh+": -> "+fe);
	    			continue;
	    		}
	    		identities_size ++;
	    		D_OID oid = D_OID.getBySequence(fe.oid);
	    		String value;
	    		Object obj;
	    		obj = c.address[i].value; //identities.get(i).get(0);
	    		if (obj != null) value = obj.toString();else value = null;
	    		ConstituentProperty data = new ConstituentProperty();
	    		data.value = value;
	    		data.label = fe.label; //Util.getString(identities.get(i).get(1));
	    		if (oid != null) {
		    		data.OID_name = oid.OID_name; //Util.getString(identities.get(i).get(2));
		    		data.OID = oid.sequence; //Util.getString(identities.get(i).get(3));
		    		data.explain = oid.explanation; //Util.getString(identities.get(i).get(4));
	    		}
	    		populateChild(new ConstituentsPropertyNode(data, this),0);
	    	}
		}
		if ( fixed_fields >= 1) {// email, fixed field
    		ConstituentProperty data = new ConstituentProperty();
    		data.value = this.getConstituent().email;
    		data.label = __("Email");//;Util.getString(identities.get(i).get(1));
    		//data.OID_name = Util.getString(identities.get(i).get(2));
    		//data.OID = Util.getString(identities.get(i).get(3));
    		//data.explain = Util.getString(identities.get(i).get(4));
    		populateChild(new ConstituentsPropertyNode(data, this),0);			
		}
		if( fixed_fields >= 2){// submitter
    		ConstituentProperty data = new ConstituentProperty();
    		data.label = __("Submitter");
    		String subm_ID = this.getConstituent().submitter_ID;
    		data.value = subm_ID;
    		long s_ID = Util.lval(subm_ID, -1);
    		if (s_ID > 0) {
    			D_Constituent wc = D_Constituent.getConstByLID(subm_ID, true, false);
    			wc.loadNeighborhoods(D_Constituent.EXPAND_ONE);
    			data.value = wc.getSurname()+", "+wc.getForename()+" <"+wc.getEmail()+">";
    			if ((wc.getNeighborhood()!=null) && (wc.getNeighborhood().length>0))
    				data.value += "("+wc.getNeighborhood()[0].getName_division()+":"+wc.getNeighborhood()[0].getName()+")";
    		} else {
    			data.value = "-";
    		}
   			populateChild(new ConstituentsPropertyNode(data, this),0);
		}
    	if( (fixed_fields+identities_size)!=getNchildren()) setNChildren(identities_size+ fixed_fields);
    	model.updateCensusStructure(model, getPath());
   }
	public static void advertise(ConstituentsIDNode can, String orgGID) {
		String hash = D_Constituent.getGIDHashFromGID(can.getConstituent().getC_GID());
		String org_hash = D_Organization.getOrgGIDHashGuess(orgGID);
		ClientSync.addToPayloadFix(net.ddp2p.common.streaming.RequestData.CONS, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}
   
	public void toggle_block() { //ConstituentsTree tree
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.getConstituent().getC_LID(), true, true);
			boolean blocked = constit.toggleBlock();
			constit.storeRequest();
			constit.releaseReference();
			this.getConstituent().blocked = blocked;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void toggle_broadcast() {//ConstituentsTree tree
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.getConstituent().getC_LID(), true, true);
			boolean broadcast = constit.toggleBroadcast();
			constit.storeRequest();
			constit.releaseReference();
			this.getConstituent().broadcast = broadcast;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public D_Constituent zapp() {//ConstituentsTree tree
		D_Constituent constit = D_Constituent.zapp(this.getConstituent().getC_LID());
		return constit;
	}
	
	public ConstituentData getConstituent() {
		return constituent;
	}
	public void setConstituent(ConstituentData constituent) {
		this.constituent = constituent;
	}
}