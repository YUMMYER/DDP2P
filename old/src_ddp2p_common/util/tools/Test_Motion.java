package util.tools;

import hds.ASNSyncPayload;

import java.util.ArrayList;

import util.DBInterface;
import util.P2PDDSQLException;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import config.Application;
import data.D_Motion;
import data.D_Witness;

public class Test_Motion {
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.db = new DBInterface(Application.DELIBERATION_FILE);
		System.out.println("D_Motion: main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		D_Motion d2 = D_Motion.getMotiByLID(pID, true, false);//.getOrgByLID_NoKeep(pID, true);
		
		System.out.println("\nD_Motion: main: d["+pID+"]="+d2);
		if (d2 == null) {
			System.out.println("D_Motion: no d2=: " + d2);
			return;
		}
		
		//System.out.println("\nD_Organization: main 1: ********\n");
		//d2.verifySignature();
		//System.out.println("\nD_Organization: main 2: ********\n");
		//d2.sign();
		System.out.println("\nD_Motion: main 3: ********\n");
		if (verif > 0) {
			boolean r = d2.verifySignature();
			System.out.println("\nD_Witness: main 3: verif result="+r);		
			
			try {
				D_Motion m = D_Motion.getEmpty().decode(new Decoder(d2.encode()));
				System.out.println("\nD_Witness: main 3': decoded="+m);		
				boolean _r = m.verifySignature();
				System.out.println("\nD_Witness: main 3': verif result="+_r);	
				
				D_Motion _m = D_Motion.getEmpty();
				_m.loadRemote(m, null, null, null);
				System.out.println("\nD_Witness: main 3': loaded m=" + _m);		
			} catch (ASN1DecoderFail e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (sign > 0) {
			d2.sign();
			boolean r2 = d2.verifySignature();
			System.out.println("\nD_Witness: main 4: verif result=" + r2);			
		}
		if (store > 0) d2.storeSynchronouslyNoException();
		
	}	
	
}