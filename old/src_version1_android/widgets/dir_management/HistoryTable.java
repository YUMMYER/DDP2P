package widgets.dir_management;

import static util.Util._;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Point;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.table.TableColumn;

import util.DBInterface;
import widgets.app.DDIcons;
import widgets.components.BulletRenderer;
import widgets.components.DebateDecideAction;
import config.Application;
import config.Application_GUI;
import config.Identity;
import hds.DirMessage;




import util.P2PDDSQLException;


public class HistoryTable extends JTable implements MouseListener{
	
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	//private static final int DIM_X = 400;
	//private static final int DIM_Y = 300;
	//BulletRenderer bulletRenderer = new BulletRenderer();
	//private ColorRenderer colorRenderer;
	private DefaultTableCellRenderer centerRenderer;
	private DirPanel dirPanel;

	public HistoryTable(DirPanel dirPanel) {
		super(new HistoryModel(Application.db, dirPanel));
		this.dirPanel = dirPanel;
		init();
	}
	public HistoryTable(DBInterface _db, DirPanel dirPanel) {
		super(new HistoryModel(_db, dirPanel));
		this.dirPanel = dirPanel;
		init();
	}
	public HistoryTable(HistoryModel dm) {
		super(dm);
		init();
	}
	public HistoryModel getModel(){
		return (HistoryModel)super.getModel();
	}
	
	//Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

//                if (realColumnIndex == HistoryModel.TABLE_COL_EXPIRATION) { //Epiration column
//                    Date d = new Date();
//                    
//                    tip = "Date format: [MM/DD/YYYY]";// i.e. [" + d.toLocaleString() +"] :" ;
//                           //+ getValueAt(rowIndex, colIndex);
//                } 

                return tip;
            }


	public void init(){
		//DEBUG=true;
		if(DEBUG) System.out.println("HistoryTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
	/*	this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);*/
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		getColumnModel().getColumn(HistoryModel.TABLE_COL_USED_IP).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_MSG_TYPE).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_REQUESTED_PEER).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_TIMESTAMP).setCellRenderer(renderer);
//		getColumnModel().getColumn(HistoryModel.TABLE_COL_NAME).setCellEditor(new DefaultCellEditor(getModel().getComboBox(null)));
//		getColumnModel().getColumn(HistoryModel.TABLE_COL_MODE).setCellRenderer(new DefaultTableCellRenderer());
//		getColumnModel().getColumn(HistoryModel.TABLE_COL_MODE).setCellEditor(new DefaultCellEditor(getModel().getModeComboBox()));
//		JTextField expiration = new JTextField();
//		expiration.setToolTipText("test");
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellRenderer(new PanelRenderer());
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyPanelEditor());
//      getColumnModel().getColumn(HistoryModel.TABLE_COL_EXPIRATION).setCellRenderer(new DefaultTableCellRenderer());
//	    getColumnModel().getColumn(HistoryModel.TABLE_COL_EXPIRATION).setCellEditor(new DefaultCellEditor(expiration));
	    	
	    	
		getModel().update(null,null);
		if(DEBUG) System.out.println("HistoryTable:init:done");
		//his.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		//DEBUG=false;
  	}
  	public JScrollPane scrollPane;
	public JScrollPane getScrollPane(){
        scrollPane = new JScrollPane(this);
        //scrollPane.setPreferredSize(new Dimension(400,100));
        //scrollPane.show(false);
        //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
		return scrollPane;
	}
/*	public TableCellRenderer getCellRenderer(int row, int column) {
		//if ((column == UpdatesModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == UpdatesModel.TABLE_COL_ACTIVITY)) return bulletRenderer;// when successfully connect the server
		return super.getCellRenderer(row, column);
	}
*/

	@Override
	public void mouseClicked(MouseEvent e) {
		jtableMouseReleased(e);
		viewMessage(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		jtableMouseReleased(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: mouserelease");
    	int _row; //=this.getSelectedRow();
    	int model_row = -1;
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: not popup");
    		return;
    	}
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        _row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if(_row>=0) model_row = this.convertRowIndexToModel(_row);
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup == null){
    		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: popup null");
    		return;
    	}
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    //	getModel().update(null, null);
    }
    JPopupMenu getPopup(int row, int col){
//		JMenuItem menuItem;
//    	
//    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
//    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
//    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
//    	ImageIcon upicon = DDIcons.getImpImageIcon(_("move up"));
//    	ImageIcon downicon = DDIcons.getImpImageIcon(_("move down"));
    	JPopupMenu popup = new JPopupMenu();
//    	DirCustomAction aAction;
//    	//System.out.println(importicon.toString());
//    	aAction = new DirCustomAction(this, _("Add a Term"), addicon,_("Add a Term"), _("AddTerm"),KeyEvent.VK_A, DirCustomAction.M_ADD);
//    	aAction.putValue("row", new Integer(row));
//    	menuItem = new JMenuItem(aAction);
//    	popup.add(menuItem);
//    	
//    	aAction = new DirCustomAction(this, _("Move UP"), upicon,_("Move UP"), _("MoveUP"),KeyEvent.VK_U, DirCustomAction.M_UP);
//    	aAction.putValue("row", new Integer(row));
//    	menuItem = new JMenuItem(aAction);
//    	popup.add(menuItem);
//    	
//    	aAction = new DirCustomAction(this, _("Move Down"), downicon,_("Move Down"), _("MoveDown"),KeyEvent.VK_O, DirCustomAction.M_DOWN);
//    	aAction.putValue("row", new Integer(row));
//    	menuItem = new JMenuItem(aAction);
//    	popup.add(menuItem);
//    	
//    	aAction = new DirCustomAction(this, _("Delete"), delicon,_("Delete Mirror"), _("Delete"),KeyEvent.VK_D, DirCustomAction.M_DELETE);
//    	aAction.putValue("row", new Integer(row));
//    	menuItem = new JMenuItem(aAction);
//    	popup.add(menuItem);
//    	
    
    	return popup;
	}
	private void viewMessage(MouseEvent e) {
		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: mouserelease");
    	int _row; //=this.getSelectedRow();
    	int model_row = -1;
    	int col; //=this.getSelectedColumn();
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = e.getPoint();
        _row = this.rowAtPoint(point);
        col = this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if (_row>=0) model_row = this.convertRowIndexToModel(_row);
        
        DirMessage msg = getModel().getObject(model_row);
        String _msg = msg+"";
        if (msg != null) _msg = "MSG:"+msg.MsgType+": "+msg.msg;
        Application_GUI.warning(_msg, "Message");
	}
	
	private void initColumnSizes() {
		this.rowHeight=20;
        HistoryModel model = (HistoryModel)this.getModel();
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
/*	public JTable getSubTable() {
		if(dirPanel==null) return null;
		return updatesPanel.getTestersTable();
	}
*/
}

