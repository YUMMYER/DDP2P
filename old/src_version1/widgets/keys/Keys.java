package widgets.keys;

import static util.Util._;

import hds.DebateDecideAction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
//import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
//import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import ciphersuits.Cipher;
import ciphersuits.SK;

import config.Application;
import config.DDIcons;
import data.D_PeerAddress;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.P2PDDSQLException;
import util.Util;
//import widgets.org.ColorRenderer;
import widgets.components.TableUpdater;

@SuppressWarnings("serial")
public class Keys extends JTable implements MouseListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;

	public Keys() {
		super(new KeysModel(Application.db));
		if(DEBUG) System.out.println("Orgs: constr from db");
		init();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
		return scrollPane;
	}
	public JPanel getPanel() {
	    	JPanel jp = new JPanel(new BorderLayout());
	    	JScrollPane scrollPane = getScrollPane();
	        //scrollPane.setPreferredSize(new Dimension(400, 200));
	        scrollPane.setPreferredSize(new Dimension(DIM_X, DIM_Y));
	        jp.add(scrollPane, BorderLayout.CENTER);
			return jp;
	}

	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		//centerRenderer = new DefaultTableCellRenderer();
		//centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
	}

	@Override
	public TableCellEditor getCellEditor(int row, int columnIndex) {
		int column = getModel().globalColumn(columnIndex);
		if(column == KeysModel.TABLE_COL_CONS){
			Object value = super.getValueAt(row, column);
			if((value != null)&&(!"".equals(value))) {
				int row_m = this.convertRowIndexToModel(row);
				value = getModel().getConstituentsValue(row_m);
				if(value instanceof JComboBox) {
					return new DefaultCellEditor((JComboBox)value);
				}
				if(value!=null) return getDefaultEditor(value.getClass());
			}
		}
		return super.getCellEditor(row, column);
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
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
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	KeysModel model = getModel();
    	KeysCustomAction cAction;
    	
    	cAction = new KeysCustomAction(this, _("Refresh!"), addicon,_("Refresh."), _("Refresh"),KeyEvent.VK_R, KeysCustomAction.C_REFRESH);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new KeysCustomAction(this, _("Remove Column!"), delicon,_("Remove Column."), _("Remove Column"), KeyEvent.VK_C, KeysCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new KeysCustomAction(this, _("Add Column!"), addicon,_("Add Column."), _("Add Column"), KeyEvent.VK_A, KeysCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	if(model.hide){
    		cAction = new KeysCustomAction(this, _("UnHide!"), addicon,_("UnHide."), _("UnHide"),KeyEvent.VK_H, KeysCustomAction.C_UNHIDE);
    	}else{
    		cAction = new KeysCustomAction(this, _("Hide!"), addicon,_("Hide."), _("Hide"),KeyEvent.VK_H, KeysCustomAction.C_HIDE);
    	}
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	if(row>=0){
	    	popup.addSeparator();

	    	if(model.getPeer(row)==null){
	    		cAction = new KeysCustomAction(this, _("Create Peer!"), addicon,_("Create Peer."), _("Create Peer"),KeyEvent.VK_P, KeysCustomAction.C_PEER);
	    		cAction.putValue("row", new Integer(row));
	    		popup.add(new JMenuItem(cAction));
	    	}
	    	
	    	cAction = new KeysCustomAction(this, _("Delete!"), delicon,_("Delete."), _("Delete"),KeyEvent.VK_D, KeysCustomAction.C_DELETE);
	    	cAction.putValue("row", new Integer(row));
	    	popup.add(new JMenuItem(cAction));
    	}
    	return popup;
	}
	public KeysModel getModel(){
		return (KeysModel) super.getModel();
	}
	void initColumnSizes() {
        KeysModel model = (KeysModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, i),
                                 false, false, 0, i);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + i + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
 
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }
}

@SuppressWarnings("serial")
class KeysCustomAction extends DebateDecideAction {
	public static final int C_REFRESH = 1;
    public static final int C_HIDE = 2;
    public static final int C_UNHIDE = 3;
    public static final int C_DELETE = 4;
    public static final int C_PEER = 5;
	public static final int C_ACOLUMN = 6;
	public static final int C_RCOLUMN = 7;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Keys tree; ImageIcon icon;
	int command;
    public KeysCustomAction(Keys tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
        this.command = command;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
    	}
    	KeysModel model = (KeysModel)tree.getModel();
    	if(command == C_REFRESH) {
    		model.update(null, null);
    		tree.initColumnSizes();
    	}else if(command == C_HIDE) {
    		model.hide = true;
    		model.update(null, null);
    	}else if(command == C_UNHIDE) {
        	model.hide = false;
        	model.update(null, null);
        }else	if(command == C_DELETE) {
        	model.delete(row);
        }else	if(command == C_PEER) {
        	model.createPeer(row);
        }else	if(command == C_RCOLUMN) {
        	model.removeColumn(row);
    		tree.initColumnSizes();
        }else	if(command == C_ACOLUMN) {
        	int col = Application.ask(_("Add"), _("Columns"), Arrays.copyOf(KeysModel.columnNames, KeysModel.columnNames.length, new Object[]{}.getClass()), null, null);
        	if(col == JOptionPane.CLOSED_OPTION) return;
        	if(col < 0) return;
       		model.addColumn(col);
    		tree.initColumnSizes();
        }
    }
}

@SuppressWarnings("serial")
class KeysModel  extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_TYPE = 1;
	public static final int TABLE_COL_PREF_DATE = 2;
	public static final int TABLE_COL_CONS = 3;
	public static final int TABLE_COL_PEER = 4;
	public static final int TABLE_COL_ORG = 5;
	public static final int TABLE_COL_HIDE = 6;
	public static final int TABLE_COL_ID = 7;
	public static final int TABLE_COL_CREA_DATE = 8;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	
	static public boolean hide = false;
	static public boolean inited_statics = false;
	static String columnNames[];
	static ArrayList<Integer> _ColumnNames;

	static public ArrayList<Integer> prep(){
		ArrayList<Integer> _ColumnNames = new ArrayList<Integer>();
		for(int i=0; i<columnNames.length; i++) _ColumnNames.add(new Integer(i));
		return _ColumnNames;
	}
	static public void init_statics(){
		if(inited_statics) return;
		inited_statics = true;
		columnNames = new String[]{_("Name"),_("Type"),_("P_Date"),_("Constituents"),_("Peer"),_("Organization"),_("Hide"),_("ID"),_("C_Date")};
		_ColumnNames = prep();
	}
	
	/**
	 * Call this to remove a current column
	 * @param crt_col
	 */
	public void removeColumn(int crt_col){
		_ColumnNames.remove(crt_col);
		this.fireTableStructureChanged();
	}
	/**
	 * Call this to add a global column index
	 * @param global_col
	 */
	public void addColumn(int global_col){
		if(_ColumnNames.contains(new Integer(global_col)))
			return;
		_ColumnNames.add(new Integer(global_col));
		this.fireTableStructureChanged();
	}
	
	ArrayList<ArrayList<Object>> _keydata = new ArrayList<ArrayList<Object>>();
	ArrayList<Component> tables= new ArrayList<Component>();
	private DBInterface db;
	private Object monitor_keydata = new Object();
	public KeysModel(DBInterface _db) {
		init_statics();
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.key.TNAME)), null);
		update(null, null);
	}
	public void createPeer(int row) {
		try {
			D_PeerAddress peer =
					new D_PeerAddress(Util.getString(_keydata.get(row).get(table.key.COL_PK)));
			if(peer._peer_ID>0){
				int v = Application.ask(_("Peer already exists, do you want to change its name?"), _("Existing Peer!"), JOptionPane.OK_CANCEL_OPTION);
				if(v!=0)
					return;
			}
			String name  = Application.input(_("What name do you want for the peer"), _("Creating peer"),
					JOptionPane.QUESTION_MESSAGE);
			peer.component_basic_data.name = name;
			peer.component_basic_data.creation_date = Util.CalendargetInstance();
			SK sk = Cipher.getSK(Util.getString(_keydata.get(row).get(table.key.COL_SK)));
			peer.sign(sk);
			peer.storeVerified();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public Object getPeer(int row) {
		if(row<0) return null;
		return _keydata.get(row).get(SELECT_COL_PEER);
	}
	public void delete(int row) {
		String ID = Util.getString(_keydata.get(row).get(table.key.COL_ID));
		int v = Application.ask(_("Are you sure that you want to delete key: "+ID), _("Delete Key"), JOptionPane.OK_CANCEL_OPTION);
		if(v != 0) return;
		try {
			db.delete(table.key.TNAME,
					new String[]{table.key.key_ID},
					new String[]{ID},
					DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void setTable(Keys keys) {
		tables.add(keys);
	}
	static String sql_nohide = 
			"SELECT "+Util.setDatabaseAlias(table.key.fields_list,"k")+
			",p."+table.peer.name+
			",o."+table.organization.name+
			",COUNT(c."+table.constituent.constituent_ID+")"+
			",c."+table.constituent.forename+
			",c."+table.constituent.name+
			",oc."+table.organization.name+
			//",c."+table.constituent.constituent_ID+
			//",SUM(CASE c."+table.constituent.constituent_ID+" WHEN NULL 0 ELSE 1 END)"+
			" FROM "+table.key.TNAME+" AS k"+
			" LEFT JOIN "+table.peer.TNAME+" AS p ON (p."+table.peer.global_peer_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.global_organization_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON (c."+table.constituent.global_constituent_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS oc ON (c."+table.constituent.organization_ID+"=oc."+table.organization.organization_ID+")"+
			" GROUP BY k."+table.key.key_ID+", k."+table.key.hide
			;
	static String sql_hide = sql_nohide+
			" HAVING (k."+table.key.hide+" != '1') OR (k."+table.key.hide+" IS NULL)";
	static int SELECT_COL_PEER = table.key.FIELDS;
	static int SELECT_COL_ORG = table.key.FIELDS+1;
	static int SELECT_COL_CONST_NB = table.key.FIELDS+2;
	static int SELECT_COL_CONST_FORENAME = table.key.FIELDS+3;
	static int SELECT_COL_CONST_NAME = table.key.FIELDS+4;
	static int SELECT_COL_CONST_ORG = table.key.FIELDS+5;
	//static int SELECT_COL_CONST_ID = table.key.FIELDS+3;
	//static int SELECT_COL_CONST_NB2 = table.key.FIELDS+3;
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		
		ArrayList<ArrayList<Object>> keydata;
		try {
			if(hide)
				keydata = db.select(sql_hide, new String[]{}, DEBUG);
			else
				keydata = db.select(sql_nohide, new String[]{}, DEBUG);
			
			synchronized(monitor_keydata){
				_keydata = keydata;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//this.fireTableDataChanged();
		new TableUpdater(this, null, tables);
	}
	public boolean isCellEditable(int row, int columnIndex) {
		int col=globalColumn(columnIndex);
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_HIDE:
		case TABLE_COL_CONS:
			return true;
		}
		return false;
	}

	@Override
	public int getColumnCount() {
		//return columnNames.length;
		return _ColumnNames.size();
	}
	@Override
	public Class<?> getColumnClass(int column) {
		int col = globalColumn(column);
		if(col == TABLE_COL_HIDE) return Boolean.class;
		if(col == TABLE_COL_CONS) return Integer.class;
		if(col == TABLE_COL_ID) return Integer.class;
		
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _keydata.size();
	}

	public Object getConstituentsValue(int row) {
		long consts = Util.lval(_keydata.get(row).get(SELECT_COL_CONST_NB));
		if(consts <= 1) return getValueAt(row, TABLE_COL_CONS);
		int S_COL_CONST_NAME = 0;
		int S_COL_CONST_FORENAME = 1;
		int S_COL_CONST_ORG = 2;
		String sql =
				"SELECT "+
						" c."+table.constituent.name+
						",c."+table.constituent.forename+
						",o."+table.organization.name+
						" FROM "+table.constituent.TNAME+" AS c"+
						" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+") "+
						" WHERE c."+table.constituent.global_constituent_ID+"=?;"
						;
		try {
			ArrayList<ArrayList<Object>> r = db.select(sql, new String[]{Util.getString(_keydata.get(row).get(table.key.COL_PK))}, DEBUG);
			ArrayList<String> list = new ArrayList<String>();
			for(ArrayList<Object> o:r){
				Object forename = o.get(S_COL_CONST_FORENAME);
				if(forename!=null) forename = ", "+forename;
				else forename="";
				list.add(
				o.get(S_COL_CONST_ORG)+":"+
				o.get(S_COL_CONST_NAME)+forename);
			}
			return new JComboBox<Object>(list.toArray());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	int globalColumn(int crt_col){
		return _ColumnNames.get(crt_col).intValue();
		// return __column.get(_columnNames.get(crt_col)).intValue();
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		synchronized(monitor_keydata){
			return s_getValueAt(rowIndex, columnIndex);
		}
	}
	private Object s_getValueAt(int rowIndex, int columnIndex) {
		if(rowIndex>=_keydata.size()) return null;
		//switch(columnIndex){
		switch(globalColumn(columnIndex)){
		case TABLE_COL_NAME:
			return _keydata.get(rowIndex).get(table.key.COL_NAME);
		case TABLE_COL_TYPE:
			return _keydata.get(rowIndex).get(table.key.COL_TYPE);
		case TABLE_COL_PREF_DATE:
			return _keydata.get(rowIndex).get(table.key.COL_PREF_DATE);
		case TABLE_COL_CREA_DATE:
			return _keydata.get(rowIndex).get(table.key.COL_CREATION_DATE);
		case TABLE_COL_HIDE:
			Object h = _keydata.get(rowIndex).get(table.key.COL_HIDE);
			return new Boolean(Util.stringInt2bool(h, false));
			
		case TABLE_COL_PEER:
			return _keydata.get(rowIndex).get(SELECT_COL_PEER);
		case TABLE_COL_ORG:
			return _keydata.get(rowIndex).get(SELECT_COL_ORG);
		case TABLE_COL_CONS:
			//if(consts == 1)	consts = (_keydata.get(rowIndex).get(SELECT_COL_CONST_ID)==null)?0:1;
			long consts = Util.lval(_keydata.get(rowIndex).get(SELECT_COL_CONST_NB));
			if(consts<=0) return "";//"#"+consts;
			Object forename = _keydata.get(rowIndex).get(SELECT_COL_CONST_FORENAME);
			if(forename!=null) forename = ", "+forename;
			else forename="";
			return "#"+consts+":/"+
			_keydata.get(rowIndex).get(SELECT_COL_CONST_ORG)+":"+
			_keydata.get(rowIndex).get(SELECT_COL_CONST_NAME)+forename;
			//return new Integer((int)consts);
		case TABLE_COL_ID:
			long id = Util.lval(_keydata.get(rowIndex).get(table.key.COL_ID));
			return new Integer((int)id);
		}
		return null;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		synchronized(monitor_keydata){
			s_setValueAt(value, row, col);
		}
	}
	private void s_setValueAt(Object value, int row, int col) {
		//switch(col) {
		switch(globalColumn(col)){
		case TABLE_COL_NAME:
			set_my_data(table.key.name, Util.getString(value), row);
			//set_my_data(table.key.preference_date, Util.getGeneralizedTime(), row);
			break;
		case TABLE_COL_HIDE:
			String val = Util.stringInt2bool(value, false)?"1":"0";
			set_my_data(table.key.hide, val, row);
			//set_my_data(table.key.preference_date, Util.getGeneralizedTime(), row);
			break;
		}
		//fireTableCellUpdated(row, col);
		//fireTableCellUpdated(row, TABLE_COL_DATE);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _keydata.size()) return;
		if("".equals(value)) value = null;
		if(_DEBUG)System.out.println("Set value =\""+value+"\"");
		String _ID = Util.getString(_keydata.get(row).get(table.key.COL_ID));
		try {
			db.update(table.key.TNAME, new String[]{field_name, table.key.preference_date},
					new String[]{table.key.key_ID}, new String[]{value, Util.getGeneralizedTime(), _ID}, _DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getColumnName(int crt_col) {
		String result = columnNames[globalColumn(crt_col)].toString();
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+crt_col+"]="+result);
		//return columnNames[crt_col].toString();
		//return _columnNames.get(crt_col).toString();
		return result;
	}
}
