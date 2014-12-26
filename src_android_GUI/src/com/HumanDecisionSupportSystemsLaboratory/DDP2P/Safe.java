/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
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
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

import hds.PeerInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

import ciphersuits.Cipher;
import ciphersuits.CipherSuit;
import ciphersuits.PK;
import ciphersuits.SK;

import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.D_Peer;

public class Safe extends android.support.v4.app.ListFragment implements
		OnItemClickListener {
	// public final static String P_SAFE_ID = "Safe_ID";
	public final static String TAG = "safe";
	public final static String P_SAFE_LID = "Safe_LID";
	public final static String P_SAFE_GIDH = "Safe_GIDH";
	public final static String P_SAFE_WHO = "who";
	public final static String SAFE_LIST_NAME = "name";
	public final static String P_SAFE_PIMG = "profImg";
	public final static String SAFE_LIST_EMAIL = "email";
	public final static String SAFE_LIST_SLOGAN = "slogan";
	protected static final String SAFE_TEXT_MY_HEADER_SEP = " | ";
	protected static final String SAFE_TEXT_ANDROID_SUBJECT_SEP = " - ";
	private String data[][];
	private ArrayList<Bitmap> imgData;

	private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
	private SimpleAdapter simpleAdapter = null;
	
	private static SafeAdapter safeAdapter = null;

	private static ArrayList<D_Peer> peers = new ArrayList<D_Peer>();

	public static ArrayList<D_Peer> getPeers() {
		return peers;
	}

	void testPeerCreation() {
		Log.d("testPeerCreation", "Safe: testPeerCreation: start");
		boolean DEBUG = true;
		CipherSuit cs = new CipherSuit(null);
		PeerInput pi = new PeerInput();
		pi.name = "Dong";
		pi.email = "dong@Hang.org";
		pi.slogan = "slogan";
		System.out.println("Android_GUI: createPeer: in " + pi);
		// cs.ciphersize = ECDSA.P_119;
		cs.cipher = Cipher.RSA;
		cs.hash_alg = Cipher.MD5;
		cs.ciphersize = 2000;
		// System.out.println("Android_GUI: createPeer: new");
		Log.d("testPeerCreation", "Safe: testPeerCreation: cipher defined");
		Cipher cif = Cipher.getNewCipher(cs, "New");
		// System.out.println("Android_GUI: createPeer: created");
		Log.d("testPeerCreation", "Safe: testPeerCreation: cipher generated");

		String date = Util.getGeneralizedTime();
		String name = pi.name;
		SK _sk = cif.getSK();
		try {
			DD.storeSK(cif, name, date);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		Log.d("testPeerCreation", "Safe: testPeerCreation: cipher generated");
		// System.out.println("Android_GUI: createPeer: stored");
		PK new_pk = cif.getPK();
		String new_gid = Util.getKeyedIDPK(new_pk);
		// String
		// _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
		if (DEBUG)
			System.out.println("CreatePeer:LoadPeer: will load=" + new_gid);
		System.out.println("Android_GUI: createPeer: load");

		Log.d("testPeerCreation", "Safe: testPeerCreation: create empty peer");
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(new_gid, null, true, true,
				true, null);// new D_Peer(new_gid);

		if (peer.getLIDstr() == null) {
			if (DEBUG)
				System.out.println("CreatePeer:LoadPeer: loaded ID=null");
			// PeerInput data = file_data[0];//new CreatePeer(DD.frame,
			// file_data[0], false).getData();
			if (DEBUG)
				System.out.println("CreatePeer:LoadPeer: loaded ID data set");
			peer.setPeerInputNoCiphersuit(pi);
			Log.d("testPeerCreation",
					"Safe: testPeerCreation: null peer loaded with pi");
		} else {
			Log.d("testPeerCreation", "Safe: testPeerCreation: no-null peer");
		}

		if (DEBUG)
			System.out.println("CreatePeer:LoadPeer: will make instance");
		peer.makeNewInstance();
		if (DEBUG)
			System.out.println("CreatePeer:LoadPeer: will sign peer");
		Log.d("testPeerCreation",
				"Safe: testPeerCreation: no-null peer instance");
		if (true) { // STORE_SIGN_AND_UNKEPT_PEER) {
			peer.sign(_sk);
			peer.storeRequest();
			peer.releaseReference();
		}
		Log.d("testPeerCreation", "Safe: testPeerCreation: peer signed");

		System.out.println("Android_GUI: createPeer: exit");

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d("onCreateView", "Safe: onCreateView: start");
		/*
		 * very IMPORTANT! The list and peers must be clear every time when the
		 * Fragment get created, otherwise, duplicate entries might occur!!!
		 */
		list.clear();
		peers.clear();

		synchronized (Orgs.monitor) {
			// pull out all safes from database
			if (Application_GUI.dbmail == null)
				Application_GUI.dbmail = new Android_DB_Email(
						this.getActivity());
			if (Application_GUI.gui == null)
				Application_GUI.gui = new Android_GUI();
			if (Application.db == null) {
				try {

					DBInterface db = new DBInterface("deliberation-app.db");
					Application.db = db;
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
				}
			}
		}
		Log.d("onCreateView", "Safe: onCreateView: database loaded");

		ArrayList<ArrayList<Object>> peer_IDs = D_Peer.getAllPeers();
		Log.d("onCreateView",
				"Safe: onCreateView: found peers: #" + peer_IDs.size());

		if (peer_IDs.size() == 0) {
			// testPeerCreation();
			// peer_IDs = D_Peer.getAllPeers();
			// Log.d("onCreateView", "Safe: onCreateView: re-found peers: #" +
			// peer_IDs.size());
		} else {
			Main.startServers();
		}

		for (ArrayList<Object> peer_data : peer_IDs) {
			if (peer_data.size() <= 0)
				continue;
			String p_lid = Util.getString(peer_data.get(0));
			D_Peer peer = D_Peer.getPeerByLID(p_lid, true, false);
			if (peer == null)
				continue;
			peers.add(peer);
		}
		Log.d("onCreateView", "Safe: onCreateView: build peers data for #"
				+ peers.size());
		data = new String[peers.size()][];
		imgData = new ArrayList<Bitmap>();
		for (int k = 0; k < peers.size(); k++) {
			D_Peer p = peers.get(k);

			// if a safe has private key then use getname... to be implemented
			data[k] = new String[] { p.getName_MyOrDefault(), p.getEmail(),
					p.getSlogan_MyOrDefault()};
			
			boolean gotIcon = false;
			try {
				byte[] icon = p.getIcon();
				if (icon != null) {
					Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0,
							icon.length - 1);
					gotIcon = true;
					Log.d(TAG, "image bmp: " + bmp.toString());
					imgData.add(bmp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!gotIcon) {
				int imgPath = R.drawable.placeholder;
				Log.d(TAG, "image path: " + imgPath);
				Bitmap bmp = BitmapFactory.decodeResource(getResources(),
						imgPath);
				Log.d(TAG, "image bmp: " + bmp.toString());
				imgData.add(bmp);
			}
		}

		/*
		 * Identity.init_Identity();
		 * 
		 * HandlingMyself_Peer.loadIdentity(null);
		 * 
		 * try { DD.startUServer(true, Identity.current_peer_ID);
		 * DD.startServer(false, Identity.current_peer_ID);
		 * DD.startClient(true);
		 * 
		 * } catch (NumberFormatException | P2PDDSQLException e) {
		 * System.err.println("Safe: onCreateView: error"); e.printStackTrace();
		 * }
		 * 
		 * try { DD.load_listing_directories(); } catch (NumberFormatException |
		 * UnknownHostException | P2PDDSQLException e) { e.printStackTrace(); }
		 * D_Peer myself = HandlingMyself_Peer.get_myself();
		 * myself.addAddress(Identity.listing_directories_addr.get(0), true,
		 * null);
		 */

		// end of data pulling out

		Log.d("onCreateView", "Safe: onCreateView: fill GUI list");
		// using a map datastructure to store data for listview
/*		for (int i = 0; i < this.data.length; i++) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("name", this.data[i][0]);
			map.put("email", this.data[i][1]);
			map.put("slogan", this.data[i][2]);

			map.put("pic", String.valueOf(R.drawable.placeholder));

			this.list.add(map);
		}*/
		
		for (int i = 0; i < this.data.length; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(SAFE_LIST_NAME, this.data[i][0]);
			map.put(SAFE_LIST_EMAIL, this.data[i][1]);
			map.put(SAFE_LIST_SLOGAN, this.data[i][2]);

			map.put("pic", String.valueOf(R.drawable.placeholder));

			this.list.add(map);
		}	
		
		safeAdapter = new SafeAdapter(getActivity(), list, imgData);

		// set up simple adapter for listview
/*		this.simpleAdapter = new SimpleAdapter(getActivity(), this.list,
				R.layout.safe_list, new String[] { "pic", "name", "email",
						"slogan", "score" }, new int[] { R.id.safe_list_pic,
						R.id.safe_list_name, R.id.safe_list_email,
						R.id.safe_list_slogan });*/
		
/*		this.setListAdapter(simpleAdapter);*/
		this.setListAdapter(safeAdapter);

		Log.d("onCreateView", "Safe: onCreateView: almost done");
		View result = super.onCreateView(inflater, container,
				savedInstanceState);
		Log.d("onCreateView", "Safe: onCreateView: done");
		return result;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		getListView().setOnItemClickListener(this);

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		Intent myIntent = new Intent();
		myIntent.setClass(getActivity(), SafeProfileActivity.class);
		D_Peer peer = null;

		ArrayList<D_Peer> p = Safe.getPeers();
		try {
			if (position >= p.size())
				return;
			peer = p.get(position);
		} catch (Exception e) {
			return;
		}
		// pass data to profile
		myIntent.putExtra(P_SAFE_WHO, data[position][0]);
		// myIntent.putExtra(P_SAFE_ID, position);
		myIntent.putExtra(P_SAFE_GIDH, peer.getGIDH());
		myIntent.putExtra(P_SAFE_LID, peer.getLIDstr());
		myIntent.putExtra(P_SAFE_PIMG, String.valueOf(R.drawable.placeholder));
		startActivity(myIntent);
	}

	public class SafeAdapter extends BaseAdapter {

		private Activity activity;
		private LayoutInflater inflater = null;
		private ArrayList<HashMap<String, String>> textData;
		private ArrayList<Bitmap> imgData;

		
		public SafeAdapter(Activity _activity, 
				ArrayList<HashMap<String, String>> _textData, ArrayList<Bitmap> _imgData) {
			activity = _activity;
			textData = _textData;
			imgData = _imgData;
			inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
		}
		
		@Override
		public int getCount() {
			return textData.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (convertView == null)
				v = inflater.inflate(R.layout.safe_list, null);

			ImageView img = (ImageView) v.findViewById(R.id.safe_list_pic);
			TextView name = (TextView) v.findViewById(R.id.safe_list_name);
			TextView email = (TextView) v.findViewById(R.id.safe_list_email);
			TextView slogan = (TextView) v.findViewById(R.id.safe_list_slogan);

			HashMap<String, String> text = textData.get(position);

			img.setImageBitmap(imgData.get(position));
			name.setText(text.get(SAFE_LIST_NAME));
			email.setText(text.get(SAFE_LIST_EMAIL));
			slogan.setText(text.get(SAFE_LIST_SLOGAN));

			Log.d(TAG, "name: " + text.get(SAFE_LIST_NAME));
			Log.d(TAG, "email: " + text.get(SAFE_LIST_EMAIL));
			Log.d(TAG, "slogan: " + text.get(SAFE_LIST_SLOGAN));
			Log.d(TAG, "slogan: " + imgData.get(position));
			return v;
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		safeAdapter.notifyDataSetChanged();
	}
	
	

}