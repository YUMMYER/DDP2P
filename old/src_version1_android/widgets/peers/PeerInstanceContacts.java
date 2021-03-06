package widgets.peers;

import static util.Util._;
import hds.Address;
import hds.Address_SocketResolved_TCP;
import hds.Client2;
import hds.Connection_Instance;
import hds.Connections;
import hds.Connection_Peer;
import hds.Connections_Peer_Directory;
import hds.Connections_Peer_Socket;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import util.DDP2P_ServiceRunnable;
import util.Util;
import widgets.app.DDIcons;
import widgets.components.DebateDecideAction;
import config.Application;
import data.D_Peer;

class D_PIC_Node implements TreeNode{
	private static final boolean DEBUG = false;
	public String text;
	public ArrayList<D_PIC_Node> child = new ArrayList<D_PIC_Node>();
	public D_PIC_Node parent = null;
	public String toString() {
		if (DEBUG) System.out.println("PeerContacts: toString: "+text);
		return text;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(DEBUG) System.out.println("PeerContacts: getChildAt: "+this+" return:"+childIndex);
		return child.get(childIndex);
	}

	@Override
	public int getChildCount() {
		if (DEBUG) System.out.println("PeerContacts: getChildCount: "+this+" return:"+child.size());
		return child.size();
	}

	@Override
	public TreeNode getParent() {
		if (DEBUG) System.out.println("PeerContacts: getParent: "+this+" return:"+parent);
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		if (DEBUG) System.out.println("PeerContacts: getIndex: "+this+" node="+node+" return:"+child.indexOf(node));
		return child.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		if (DEBUG) System.out.println("PeerContacts: getAllowsChildren: "+this+" true="+true);
		return true;//child.size()!=0;
	}

	@Override
	public boolean isLeaf() {
		if (DEBUG) System.out.println("PeerContacts: isLeaf: "+this+" true="+true);
		return child.size()==0;
	}

	@Override
	public Enumeration children() {
		if (DEBUG) System.out.println("PeerContacts: children: "+this);
		return Collections.enumeration(child);
	}	
}

@SuppressWarnings("serial")
public
class PeerInstanceContacts  extends JPanel implements MouseListener {
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static final String ALREADY_CONTACTED = _("Already contacted ***");
	public static JTree jt = null;
	public static JLabel l = null;
	public static JTree old_jt = null;
	public static JLabel old_l = null;
	public boolean refresh = true;
	//public boolean filter = true;
	Connections connections;
	
	String my_peer_name;
	D_Peer dpa;
	boolean pack_sockets = true;
	public PeerInstanceContacts() {
		this.setLayout(new BorderLayout());
		old_l = new JLabel(_("Current Connections (right click for a menu to update view)"));
		old_l.setHorizontalTextPosition(SwingConstants.LEFT);
		this.add(old_l, BorderLayout.NORTH);
		this.addMouseListener(this);
		//l.setHorizontalAlignment(JLabel.LEFT);
	}
	public void setConnections(Connections c) {
		connections = c;
	}
	public void update() {
		if (connections == null) this.setConnections(Client2.conn);
		if (connections == null) {
			if (_DEBUG) System.out.println("PeerContacts: update: no connections object");
			return;
		};
		
		if (DEBUG) System.out.println("PeerContacts: update: start");
		if (!refresh) {
			if (_DEBUG) System.out.println("PeerContacts: update: norefresh");
			return;
		}
		Object[] data = getTree();
		D_PIC_Node root = new D_PIC_Node(); root.text = "root";
		for (Object o: data) {
			((D_PIC_Node)o).parent = root;
			root.child.add((D_PIC_Node)o);
		}
		jt = new JTree(root);
		jt.setRootVisible(false);
		jt.expandPath(new TreePath(new Object[]{root}));
		for (Object o: data)	jt.expandPath(new TreePath(new Object[]{root, o}));
		jt.addMouseListener(this);
		EventQueue.invokeLater(new DDP2P_ServiceRunnable(this) {
			public void _run() {
				PeerInstanceContacts pic = (PeerInstanceContacts)ctx;
				String now = Util.getGeneralizedTime();
				if (DEBUG) System.out.println("PeerInstanceContacts: invoked later:do "+now);
				if (pic.old_jt != null) pic.remove(pic.old_jt);
				pic.old_jt = pic.jt;
				if (pic.jt != null) pic.add(pic.jt, BorderLayout.CENTER);
				if (pic.old_l != null) pic.remove(pic.old_l);
				pic.l = new JLabel(_("Latest Contacts for:")+" "+now);
				pic.l.setHorizontalTextPosition(SwingConstants.LEFT);
				pic.old_l = pic.l;
				pic.add(l, BorderLayout.NORTH);
				pic.revalidate();
				if (DEBUG) System.out.println("PeerContacts: invoked later:did");
			}}
		);		
	}
	private Object[] getTree() {
		ArrayList<Object> result = new ArrayList<Object>();
		for (Connection_Peer peer : connections.used_peers_AL_PC) {
			if (DEBUG) System.out.println("PeerContacts: getTree: peer="+peer);
			D_PIC_Node n = new D_PIC_Node();
			result.add(n);
			n.text = "\""+peer.getName()+"\" Contacted="+peer.contacted_since_start+" (ok="+peer.last_contact_successful+")";
			
			if (peer.shared_peer_directories.size() > 0) {
				D_PIC_Node dirs = getAddressesDirNodes(peer.shared_peer_directories);
				dirs.parent = n;
				n.child.add(dirs);
			}
			if (peer.shared_peer_sockets.size() > 0) {
				D_PIC_Node socks = getAddressesSockNodes(peer.shared_peer_sockets);
				socks.parent = n;
				n.child.add(socks);
			}
	
			for (Connection_Instance ci : peer.instances_AL) {
				if (DEBUG) System.out.println("PeerContacts: getTree: peer instance="+ci);
				D_PIC_Node  da = getInstance(ci);
				da.parent=n;
				n.child.add(da);
			}
		}
		return result.toArray();
	}
	private D_PIC_Node getInstance(Connection_Instance i) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = "INST=\""+i.dpi.peer_instance+"\" Contacted="+i.contacted_since_start+" (ok="+i.last_contact_successful+")";
		if (i.peer_directories.size() > 0) {
			D_PIC_Node dirs = getAddressesDirNodes(i.peer_directories);
			dirs.parent = n;
			n.child.add(dirs);
		}
		if (i.peer_sockets.size() > 0) {
			D_PIC_Node socks = getAddressesSockNodes(i.peer_sockets);
			socks.parent = n;
			n.child.add(socks);
		}
		return n;
	}
	private D_PIC_Node getAddressesDirNodes(ArrayList<Connections_Peer_Directory> addr) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = Address.DIR;
		for (Connections_Peer_Directory ad: addr) {
			D_PIC_Node a = new D_PIC_Node();
			a.parent = n;
			if (ad == null) Util.printCallPath("Null peer directory here!");
			String rep = ad.getReportedAddresses();
			a.text =
					ad.supernode_addr.ad+"/"+ad.supernode_addr.isa+" (#"+ad.address_ID+") rep=" + rep +
					"contact="+ ad._last_contact_TCP+" tried="+ad.contacted_since_start_TCP+"/ok="+ad.last_contact_successful_TCP
					;
			n.child.add(a);
		}
		return n;
	}	
	private D_PIC_Node getAddressesSockNodes(ArrayList<Connections_Peer_Socket> addr) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = Address.SOCKET;
		for (Connections_Peer_Socket ad: addr) {
			D_PIC_Node a = new D_PIC_Node();
			a.parent = n;
			a.text = ((ad.addr != null)?(ad.addr.ad+" (#"+ad.address_ID+") IP="+ad.addr.ia):"No addresss ")+
					" date="+ad._last_contact+
					" TCP="+ad.contacted_since_start_TCP+"(ok="+ad.last_contact_successful_TCP+" "+
					"open="+ad.tcp_connection_open+"/busy="+ad.tcp_connection_open_busy+")"+
					" UDP="+ad.contacted_since_start_UDP+"(ok="+ad.last_contact_successful_UDP+")"+
					" NAT="+ad.behind_NAT
					;
			n.child.add(a);
		}
		return n;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);		
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	JPopupMenu popup = getPopup(evt);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	private JPopupMenu getPopup(MouseEvent evt) {
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	PeerInstanceContactsAction prAction;
    	//
    	prAction = new PeerInstanceContactsAction(this, _("Refresh!"), reseticon,_("Let it refresh."),
    			_("Go refresh!"),KeyEvent.VK_R, PeerInstanceContactsAction.REFRESH);
    	//prAction.putValue("row", new Integer(model_row));
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerInstanceContactsAction(this, _("No Refresh!"), reseticon,_("Stop refresh."),
    			_("No refresh!"), KeyEvent.VK_S, PeerInstanceContactsAction.NO_REFRESH);
    	popup.add(new JMenuItem(prAction));


    	prAction = new PeerInstanceContactsAction(this, _("Reset!"), reseticon,_("Reset."),
    			_("Reset!"), KeyEvent.VK_D, PeerInstanceContactsAction.RESET);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerInstanceContactsAction(this, _("Pack Socket entries!"), reseticon,_("Pack Socket entries."),
    			_("Pack Socket!"), KeyEvent.VK_P, PeerInstanceContactsAction.PACK_SOCKET);
    	popup.add(new JMenuItem(prAction));

    	return popup;
	}
	public void connectWidget() {
		// TODO Auto-generated method stub
		
	}
	public void disconnectWidget() {
		// TODO Auto-generated method stub
		
	}
	public Component getComboPanel() {
		return this;
	}
}

@SuppressWarnings("serial")
class PeerInstanceContactsAction extends DebateDecideAction {
    public static final int REFRESH = 0;
    public static final int NO_REFRESH = 1;
	public static final int RESET = 2;
	public static final int UNFILTER = 3;
	public static final int FILTER = 4;
	public static final int PACK_SOCKET = 5;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	PeerInstanceContacts tree; ImageIcon icon; int command;
    public PeerInstanceContactsAction(PeerInstanceContacts tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
	//public final JFileChooser filterUpdates = new JFileChooser();
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
        if (DEBUG) System.err.println("PeerInstanceConnectionsRowAction:command property: " + command);
    	JMenuItem mnu;
		switch(command){
		case REFRESH:
			tree.refresh = true;
			tree.update();
			break;
		case NO_REFRESH:
			tree.refresh = false;
			break;
		case RESET:
			tree.update();
			break;
		case PACK_SOCKET:
			tree.pack_sockets = !tree.pack_sockets;
			tree.update();
			break;
		}
    }
}
    