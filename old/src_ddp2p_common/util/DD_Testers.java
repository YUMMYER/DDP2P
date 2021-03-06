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
package util;

import static util.Util.__;
import hds.Server;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Tester;
class TesterSaverThread extends Thread {
	DD_Testers ds;

	TesterSaverThread(DD_Testers ds){
		this.ds = ds;
	}
	public void run(){
		try {
			_run();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void _run() throws P2PDDSQLException{
    	for (D_Tester d : ds.testers) {
    		d.store();
//    		D_Tester uki = new D_Tester(d);
//			if (uki.existsInDB()) uki.store(); 
//			else uki.store();
    	}
	}
}

public class DD_Testers extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 0;
	ArrayList<D_Tester> testers = new ArrayList<D_Tester>();
	

	public boolean empty() {
		return testers == null || testers.size() == 0;
	}
	@Override
	public void save() {
		if(DEBUG) System.out.println("DD_Testers:save");
		new TesterSaverThread(this).start();
    	if (true) {
    		Application_GUI.warning(__("Adding testers:")+" \n"+this.toString(), __("Import"));
    		//return;
    	}
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_Testers:decode "+asn1.length);
		decode(new Decoder(asn1));
		if(DEBUG)System.out.println("DD_Testers:decoded");
		if (empty()) throw new ASN1.ASNLenRuntimeException("Empty DD_Testers");
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return this.toString();
	}

	@Override
	public String getString() {
		return "DD_Testers:\n"+Util.concat(testers, "\nAND\n","");
	}

	public void add(String name, String email, String public_key, String description, String url){
		D_Tester d = new D_Tester();
		d.name = name;
		d.email = email;
		d.testerGID = public_key;
		d.description = description;
		d.url = url;
		testers.add(d);
		if(DEBUG) System.out.println("DD_Testers:added:"+d);
	}
	public void add(D_Tester t){
		testers.add(t);
	}
	/**
	 * return a directory string
	 * compatible with what is stored in application table
	 */
	@Override
	public String toString() {
		return Util.concatSummary(testers, "\n",null);
	}

	/**
	 * TODO
	 *  add the directories from a string (as one from xml)
	 */
	@Override
	public boolean parseAddress(String content) {
		return false;
	}

	@Override
	public short getSignShort() {
		if(DEBUG) System.out.println("DD_Testers:get_sign=:"+DD.STEGO_SIGN_TESTERS);
		return DD.STEGO_SIGN_TESTERS;
	}
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.STEGO_SIGN_TESTERS+"");
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(Encoder.getEncoder(testers));
		//enc.setASN1Type(getASN1Type());
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}
//	public static byte getASN1Type() {
//		return DD.TAG_AC18;
//	}

	@Override
	public DD_Testers decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		testers = d.getFirstObject(true).getSequenceOfAL(D_Tester.getASNType(), new D_Tester());
		return this;
	}
	/**
	 * Export
	 * @param args
	 */
	public static void main(String[] args){
		if(args.length<2){
			System.out.println("database file");
			return;
		}
		try {
			Application.db = new DBInterface(args[0]);
			DD_Testers testers = new DD_Testers();
			ArrayList<D_Tester> tds;
			if(args.length == 2) {
				tds = D_Tester.retrieveTesterDefinitions();
			}else{
				long id = Long.parseLong(args[2]);
				tds = new ArrayList<D_Tester>();
				D_Tester t = D_Tester.getTesterInfoByLID(id);
				tds.add(t);
			}
			for (D_Tester t : tds) {
				testers.add(t);
				System.out.println("Saving: "+t);
			}
			File file = new File(args[1]);
			if(file.exists()) {
				System.out.println("File exists");
				return;
			}
			util.EmbedInMedia.saveSteganoBMP(file, testers.encode(), testers.getSignShort());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

//class Tester extends ASNObj{
//String version = "0";
//String domain;
//int port;
//@Override
//public Encoder getEncoder() {
//	Encoder enc = new Encoder().initSequence();
//	enc.addToSequence(new Encoder(version));
//	enc.addToSequence(new Encoder(domain));
//	enc.addToSequence(new Encoder(port));
//	enc.setASN1Type(getASN1Type());
//	return enc;
//}
//@Override
//public Tester decode(Decoder dec) throws ASN1DecoderFail {
//	Decoder d = dec.getContent();
//	version = d.getFirstObject(true).getString();
//	domain = d.getFirstObject(true).getString();
//	port = d.getFirstObject(true).getInteger().intValue();
//	return this;
//}
//public ASNObj instance() throws CloneNotSupportedException{return new Dir();}
//public String toString(){
//	return domain+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+port;
//}
//public static byte getASN1Type() {
//	return DD.TAG_AC17;
//}
//}
