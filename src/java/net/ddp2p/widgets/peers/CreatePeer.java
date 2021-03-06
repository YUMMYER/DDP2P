package net.ddp2p.widgets.peers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.ddp2p.ciphersuits.KeyManagement;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.components.CipherSelection;
import net.ddp2p.widgets.components.TranslatedLabel;
import static net.ddp2p.common.util.Util.__;

@SuppressWarnings("serial")
public class CreatePeer extends JDialog implements ActionListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static final String cOK = "OK";
	static final String cIMPORT = "IMPORT";
	static final String cCANCEL = "CANCEL";
	private static final String NAME_CHARACTERS = null;
	private static final String SLOGAN_CHARACTERS = null;
	private static final String EMAIL_CHARACTERS = null;
	GridBagConstraints c = new GridBagConstraints();
	CipherSelection cipherSelection = new CipherSelection();
	JButton button_ok;
	JButton button_import;
	JButton button_cancel;
	private JTextField name;
	private JTextField instance;
	private JTextField slogan;
	private boolean valid = false;
	public D_Peer peer;
	SK new_sk;
	boolean show_import = true;
	private JTextField email;
	private JLabel label_ciphersuit;
	private JTextArea description; 
	public PeerInput getData() {
		PeerInput data = new PeerInput();
		data.valid = valid;
		data.name = textNull(name.getText());
		data.slogan = textNull(slogan.getText());
		data.email = textNull(email.getText());
		data.instance = textNull(instance.getText());
		data.cipherSuite = this.cipherSelection.getSelectedCipherSuite();
		return data;
	}
	private String textNull(String text) {
		if ((text != null) && (!"".equals(text.trim()))) return text;
		return null;
	}
	private boolean isNotNull(String text) {
		if (textNull(text) == null) return false;
		return true;
	}
	public void setData(PeerInput pi) {
		if (isNotNull(pi.name)) name.setText(pi.name);
		if (isNotNull(pi.slogan)) slogan.setText(pi.slogan);
		if (isNotNull(pi.email)) email.setText(pi.email);
		if (isNotNull(pi.instance)) instance.setText(pi.instance);
	}
	/**
	 * Returns the peer (saved)
	 * @return
	 */
	public D_Peer getDPeer() {
		return peer;
	}
	/*
	public D_PeerAddress getPeerAddress() {
		Util.printCallPath("");
		System.exit(1);
		//return peer;
		return null;
	}
	*/
	public static final String title = __("Enter your data!");
	/**
	 * Whether the result should be signed, stored and unkept.
	 * Otherwise it will be kept and unsigned.
	 */
	private static final boolean STORE_SIGN_AND_UNKEPT_PEER = true;
	public CreatePeer (JFrame parent) {
		super(parent, title, true);
		init(parent);
		showIt();
	}
	public CreatePeer (JFrame parent, PeerInput initial) {
		super(parent, title, true);
		init(parent);
		initialize(initial);
		showIt();
	}
	public CreatePeer (JFrame parent, PeerInput initial, boolean _show_import) {
		super(parent, title, true);
		show_import = _show_import;
		init(parent);
		initialize(initial);
		showIt();
	}
	public void showIt(){
	    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	    pack(); 
	    setVisible(true); // this shows it;
	}
	/**
	 * Sets only name, slogan and email (if non-null)
	 * @param initial
	 */
	public void initialize(PeerInput initial) {
		if (initial.name != null) this.name.setText(initial.name);
		if (initial.slogan != null) this.slogan.setText(initial.slogan);
		if (initial.email != null) this.email.setText(initial.email);
	}
    static String _description =
    		" "+
    	    __("Please specify here the name by which your friends will see you,\nand your email address that they know.")
    		+"\n "
    		+__("They may email you at that address to verify you are indeed their friend.")
    		+"\n "
    		+__("Alternatively, you can load an identity you created & exported on another machine.")
    		+"\n "
    		+__("If you plan working on multiple machines, provide an instance identifier!")
    		;
	void init(JFrame parent) {
		//Util.printCallPath("Why?");
		//JButton bp;
	    if (parent != null) {
	    	Dimension parentSize = parent.getSize(); 
	    	Point p = parent.getLocation(); 
	    	setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
	    }
	    
	    
		int y = 0;
	    JPanel messagePane = new JPanel();
	    getContentPane().add(messagePane);

	    messagePane.setLayout(new GridBagLayout());
		c.ipadx=10; c.gridx=0; c.gridy=y; c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;

	    c.gridx = 0; c.gridy = y; //c.gridwidth = 2;
		messagePane.add(new JLabel(__("Description")), c);
	    c.gridx = 1;
		messagePane.add(description = new JTextArea(_description), c);
		description.setRows(3);
		description.setColumns(30);
	    y++;

		
	    c.gridx = 0; c.gridy = y; //c.gridwidth = 1;
		messagePane.add(new JLabel(__("Peer Name")), c);
	    c.gridx = 1;
	    messagePane.add(name = new JTextField(NAME_CHARACTERS), c);
	    y++;
	    
	    c.gridx = 0; c.gridy = y;
	    String peerInstance = __("Device Name"); //_("Peer Instance");
	    messagePane.add(new JLabel(peerInstance), c);
	    c.gridx = 1; c.gridy = y;
	    messagePane.add(instance = new JTextField(SLOGAN_CHARACTERS), c);
	    y++;
	    
	    c.gridx = 0; c.gridy = y;
	    String statusText = __("Status Text"); //_("Peer Slogan");
	    messagePane.add(new JLabel(statusText), c);
	    c.gridx = 1; c.gridy = y;
	    messagePane.add(slogan = new JTextField(SLOGAN_CHARACTERS), c);
	    y++;
	    
	    c.gridx = 0; c.gridy = y;
	    messagePane.add(new JLabel(__("Email For Verification")), c);
	    c.gridx = 1; c.gridy = y;
	    messagePane.add(email = new JTextField(EMAIL_CHARACTERS), c);
	    y++;
	      
	    c.gridx = 0; c.gridy = y;
	    messagePane.add(label_ciphersuit = new JLabel(__("Cipher-Suit")), c);
	    c.gridx = 1;
	    this.cipherSelection = new CipherSelection();			
	    messagePane.add(cipherSelection, c);
	    y++;

	      
	    JPanel buttonPane = new JPanel();
	    
	    button_ok = new JButton("CREATE");
	    button_ok.setActionCommand(cOK);
	    buttonPane.add(button_ok);
	    button_ok.addActionListener(this);
	    
	    if (show_import) {
		    button_import = new JButton("IMPORT");
		    button_import.setActionCommand(cIMPORT);
		    buttonPane.add(button_import);
		    button_import.addActionListener(this);
	    }
	    
	    button_cancel = new JButton("CANCEL");
	    button_cancel.setActionCommand(cCANCEL);
	    buttonPane.add(button_cancel);
	    button_cancel.addActionListener(this);
	    
	      
	    getContentPane().add(buttonPane, BorderLayout.SOUTH);
	}
	int getIndex(String[] items, String val){
		for (int i=0; i<items.length; i++) if(items[i].equals(val)) return i;
		return -1;
	}
	/**
	 * Creates a peer based on importing a file. The peer is signed and saved.
	 * It is OK to return it unsigned and  keep (as that has to be verified by caller)
	 * @return
	 */
	public D_Peer loadPeer() {
		if (DEBUG) System.out.println("CreatePeer:LoadPeer: start");
		D_Peer peer = null;
		JFileChooser filterUpdates = new JFileChooser();
		filterUpdates.setFileFilter(new net.ddp2p.widgets.components.UpdatesFilterKey());
		filterUpdates.setName(__("Select Secret Trusted Key"));
		//filterUpdates.setSelectedFile(null);
		Util_GUI.cleanFileSelector(filterUpdates);
		int loadNewPeerVal = filterUpdates.showDialog(this,__("Specify Trusted Secret Key File"));
		if (loadNewPeerVal != JFileChooser.APPROVE_OPTION){
			if (_DEBUG) System.out.println("CreatePeer:LoadPeer: cancelled");
			return null;
		}
		File fileLoadPEER = filterUpdates.getSelectedFile();
		if (!fileLoadPEER.exists()) {
			if (_DEBUG) System.out.println("CreatePeer:LoadPeer: inexistant file: "+fileLoadPEER);
			Application_GUI.warning(__("Inexisting file: "+fileLoadPEER.getPath()), __("Inexisting file!"));
			return null;
		}
		if (DEBUG) System.out.println("CreatePeer:LoadPeer: choice="+fileLoadPEER);
		try{
			String []__pk = new String[1];
			PeerInput file_data[] = new PeerInput[]{new PeerInput()};
			String _file_data[] = new String[]{null};
			boolean is_new[] = new boolean[1];
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: will load pk");
			new_sk = KeyManagement.loadSecretKey(fileLoadPEER.getCanonicalPath(), __pk, _file_data, is_new);
			file_data[0].name = _file_data[0];
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: loaded sk");
			if (new_sk == null) {
				Application_GUI.warning(__("Failure to load key!"), __("Loading Secret Key"));
				return null;
			}
			/*
			if (!is_new[0]) {
				Application.warning(_("Secret key already available!"), _("Loading Secret Key"));
				return null;
			}
			*/
			PK new_pk = new_sk.getPK();
			String new_gid = Util.getKeyedIDPK(new_pk);
			//String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: will load="+new_gid);
			peer = D_Peer.getPeerByGID_or_GIDhash(new_gid, null, true, true, true, null);//  new D_Peer(new_gid);
			//peer = D_Peer.getPeerByPeer_Keep(peer);
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: loaded peer="+peer);
			if (peer.getLIDstr() == null) {
				if (DEBUG) System.out.println("CreatePeer:LoadPeer: loaded ID=null");
				PeerInput data = file_data[0];//new CreatePeer(DD.frame, file_data[0], false).getData();
				if (DEBUG) System.out.println("CreatePeer:LoadPeer: loaded ID data set");
				peer.setPeerInputNoCiphersuit(data);
			}
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: will make instance");
			peer.makeNewInstance();
			
			//if (isMyself(peer)){setInstance} //cannot be since I had no key
			//peer.component_basic_data.globalID = _pk;
			//peer.component_basic_data.globalIDhash=null;
			//peer._peer_ID = -1;
			//peer.peer_ID = null;
			if (DEBUG) System.out.println("CreatePeer:LoadPeer: will sign peer");
			if (STORE_SIGN_AND_UNKEPT_PEER) {
				peer.sign(new_sk);
				peer.storeRequest();
				peer.releaseReference();
			}
			//peer.storeAsynchronouslyNoException();
		}catch(Exception e2){
			e2.printStackTrace();
			if (_DEBUG) System.out.println("CreatePeer:LoadPeer: exception");
			return null;
		}
		return peer;
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button_ok) {
			if (DEBUG) System.out.println("CreatePeer: action: dok");
			valid = true;
			if (peer != null) {
				peer = D_Peer.getPeerByPeer_Keep(peer);
				peer.setPeerInputNoCiphersuit(this.getData());
				if (STORE_SIGN_AND_UNKEPT_PEER) {
					peer.sign(new_sk);
					peer.storeRequest(); //storeAsynchronouslyNoException();
					peer.releaseReference();
				}
			}
		}
		if (e.getSource() == button_import) {
			//setVisible(false); 
			peer = loadPeer();
			if ( peer == null ) {
				if (DEBUG) System.out.println("CreatePeer: action: peer null");
				return;
			}
			PeerInput dta = PeerInput.getPeerInput(peer);
			this.setData(dta);
			this.cipherSelection.setVisible(false);
			this.label_ciphersuit.setVisible(false);
			//valid = true;
			if (DEBUG) System.out.println("CreatePeer: action: peer valid");
			return;
		}
		setVisible(false); 
		dispose(); 
		if (DEBUG) System.out.println("CreatePeer: action: dispose");
	}
}
