/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * DataSourceViewer.java
 *
 * Created on October 15, 2003, 1:03 PM
 */

package tufts.vue;
/**
 *
 * @author  akumar03
 */

import tufts.vue.gui.VueButton;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;
import java.io.*;
import java.util.*;
import java.net.URL;


// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;



//

public class DataSourceViewer  extends JPanel implements KeyListener{
    /** Creates a new instance of DataSourceViewer */
    
    
    
    static DRBrowser drBrowser;
    static DataSource activeDataSource;
    static JPanel resourcesPanel,dataSourcePanel;
    String breakTag = "";
    
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT = VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    
    
    
    JPopupMenu popup;       // add edit popup
    AddEditDataSourceDialog addEditDialog = null;   //  The add/edit dialog box.
    AbstractAction addAction;//
    AbstractAction editAction;
    AbstractAction deleteAction;
    AbstractAction saveAction;
    AbstractAction refreshAction;
    
    
    
    
    
    public static Vector  allDataSources = new Vector();
    
    
    public static DataSourceList dataSourceList;
    
    public DataSourceViewer(DRBrowser drBrowser){
        
        
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        
        dataSourceList = new DataSourceList(this);
        dataSourceList.addKeyListener(this);
        
        
        loadDataSources();
        
        // if (loadingFromFile)dataSourceChanged = false;
        setPopup();
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                
                if ((DataSource)((JList)e.getSource()).getSelectedValue()!=null){
                    DataSourceViewer.this.setActiveDataSource(((DataSource)((JList)e.getSource()).getSelectedValue()));
                    
                    
                }}
        });
        dataSourceList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == e.BUTTON3) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        
        // GRID: addConditionButton
        JButton addButton=new VueButton("add");
        addButton.setBackground(this.getBackground());
        addButton.setToolTipText("Add/Edit Datasource Information");
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                
            }
        });
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new VueButton("delete");
        deleteButton.setBackground(this.getBackground());
        deleteButton.setToolTipText("Remove a Datasource from VUE");
        
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteDataSource(activeDataSource);
                
                refreshDataSourceList();
                if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
                else{
                    DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                    DataSourceViewer.this.resourcesPanel  = new JPanel();
                    DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                    DataSourceViewer.this.drBrowser.repaint();
                    DataSourceViewer.this.drBrowser.validate();
                }
            }
        });
        
        
        // GRID: addConditionButton
        
        
        JButton refreshButton=new VueButton("refresh");
        
        refreshButton.setBackground(this.getBackground());
        refreshButton.setToolTipText("Refresh Local Datasource");
        
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                activeDataSource.setResourceViewer();
                refreshDataSourceList();
                
            }
        });
        
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("Add/Delete/Refresh a Data Source");
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        
        
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);
        topPanel.add(questionLabel);
        
        
        
        dataSourcePanel = new JPanel();
        dataSourcePanel.setLayout(new BorderLayout());
        dataSourcePanel.add(topPanel,BorderLayout.NORTH);
        
        
        JScrollPane dataJSP = new JScrollPane(dataSourceList);
        dataSourcePanel.add(dataJSP,BorderLayout.CENTER);
        add(dataSourcePanel,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        
        
        
    }
    
    public static void addDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else type = 6;
        
        Vector dataSourceVector = (Vector)allDataSources.get(type);
        dataSourceVector.add(ds);
        refreshDataSourceList();
        
        
    }
    
    public void deleteDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else type = 6;
        if(JOptionPane.showConfirmDialog(this,"Are you sure you want to delete DataSource :"+ds.getDisplayName(),"Delete DataSource Confirmation",JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {   
            Vector dataSourceVector = (Vector)allDataSources.get(type);
            dataSourceVector.removeElement(ds);
        }
        
        
    }
    
    public static void refreshDataSourceList(){
        
        int i =0; Vector dsVector;
        String breakTag = "";
        int NOOFTYPES = 6;
        
        
        if (!(dataSourceList.getContents().isEmpty()))dataSourceList.getContents().clear();
        
        for (i = 0; i < NOOFTYPES; i++){
            
            
            dsVector = (Vector)allDataSources.get(i);
            
            if (!dsVector.isEmpty()){
                
                
                
                int j = 0;
                for(j = 0; j < dsVector.size(); j++){
                    dataSourceList.getContents().addElement(dsVector.get(j));
                    
                }
                
                boolean breakNeeded = false; int typeCount = i+1;
                
                while ((!breakNeeded) && (typeCount < NOOFTYPES)){
                    
                    if (!((Vector)allDataSources.get(i)).isEmpty())breakNeeded = true;
                    
                    typeCount++;
                }
                
                if (breakNeeded) dataSourceList.getContents().addElement(breakTag);
                
            }
            
        }
        
        
        dataSourceList.setSelectedValue(getActiveDataSource(),true);
        
        dataSourceList.validate();
        
        
        
    }
    
    
    public static DataSource getActiveDataSource() {
        return activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){
        
        this.activeDataSource = ds;
        
        refreshDataSourcePanel(ds);
        
        
        dataSourceList.setSelectedValue(ds,true);
         
    }
    public static void refreshDataSourcePanel(DataSource ds){
        
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        
        resourcesPanel.setBorder(new TitledBorder(ds.getDisplayName()));
        
        JPanel dsviewer = (JPanel)ds.getResourceViewer();
        resourcesPanel.add(dsviewer,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
  
        
    }
    
    public void  setPopup() {
        popup = new JPopupMenu();
        
        
        addAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        editAction = new AbstractAction("Edit") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(1);
                
                
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        deleteAction =  new AbstractAction("Delete") {
            public void actionPerformed(ActionEvent e) {
                deleteDataSource(activeDataSource);
                refreshDataSourceList();
                if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
                else{
                    DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                    DataSourceViewer.this.resourcesPanel  = new JPanel();
                    DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                    DataSourceViewer.this.drBrowser.repaint();
                    DataSourceViewer.this.drBrowser.validate();
                }
                
                
            }
        };
        
        saveAction =  new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                // saveDataSourceViewer();
            }
        };
        refreshAction =  new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refreshDataSourceList();
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
        popup.addSeparator();
        popup.add(deleteAction);
        // popup.addSeparator();
        // popup.add(saveAction);
        popup.addSeparator();
        popup.add(refreshAction);
        
    }
    private boolean checkValidUser(String userName,String password,int type) {
        if(type == 3) {
            try {
                TuftsDLAuthZ tuftsDL =  new TuftsDLAuthZ();
                osid.shared.Agent user = tuftsDL.authorizeUser(userName,password);
                if(user == null)
                    return false;
                if(tuftsDL.isAuthorized(user, TuftsDLAuthZ.AUTH_VIEW))
                    return true;
                else
                    return false;
            } catch(Exception ex) {
                VueUtil.alert(null,"DataSourceViewer.checkValidUser - Exception :" +ex, "Validation Error");
                ex.printStackTrace();
                return false;
            }
        } else
            return true;
    }
    
    public void showAddEditWindow(int mode) {
        if ((addEditDialog == null)  || true) { // always true, need to work for cases where case where the dialog already exists
            addEditDialog = new AddEditDataSourceDialog();
            addEditDialog.show(mode);
        }
    }
    
    
    
    
    
    
    
    public void loadDataSources(){
        
        Vector dataSource0 = new Vector();
        Vector dataSource1 = new Vector();
        Vector dataSource2 = new Vector();
        Vector dataSource3 = new Vector();
        Vector dataSource4 = new Vector();
        Vector dataSource5 = new Vector();
        
        allDataSources.add(dataSource0);
        allDataSources.add(dataSource1);
        allDataSources.add(dataSource2);
        allDataSources.add(dataSource3);
        allDataSources.add(dataSource4);
        allDataSources.add(dataSource5);
        
        
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        
        
        try{
            SaveDataSourceViewer rViewer = unMarshallMap(f);
            Vector rsources = rViewer.getSaveDataSources();
            while (!(rsources.isEmpty())){
                DataSource ds = (DataSource)rsources.remove(0);
                System.out.println(ds.getDisplayName()+ds.getClass());
                try {
                    ds.setResourceViewer();
                    addDataSource(ds);
                    setActiveDataSource(ds);
                    
                } catch(Exception ex) {System.out.println("this is a problem in restoring the datasources");}
                
            }
            
            
        }catch (Exception ex) {
            
            VueUtil.alert(null,"Previously saved datasources file does not exist or cannot be read. Adding Default Datasources","Loading Datasources");
            DataSource ds1 = new LocalFileDataSource("My Computer","");
            addDataSource(ds1);
            DataSource ds2 = new FavoritesDataSource("My Favorites");
            addDataSource(ds2);
            DataSource ds3 = new FedoraDataSource("Tufts Digital Library","vue-dl.tccs.tufts.edu", "test","test");
            addDataSource(ds3);
            DataSource ds4 = new GoogleDataSource("Tufts Web",VueResources.getString("url.google"));
            addDataSource(ds4);
            setActiveDataSource(ds2);
            
        }
        
        
        
        
        
        
        
        refreshDataSourceList();
        
        
        
        
        
        
    }
    
    
    /*
     * static method that returns all the datasource where Maps can be published.
     * Only FEDORA @ Tufts is available at present
     */
    public static Vector getPublishableDataSources(int i) {
        Vector mDataSources = new Vector();
        try {
            mDataSources.add(new FedoraDataSource("Tufts Digital Library","vue-dl.tccs.tufts.edu","test","test"));
            
        } catch (Exception ex) {
            System.out.println("Datasources can't be loaded");
        }
        
        /**
         * Iterator i = dataSources.iterator();
         * while(i.hasNext() ) {
         * DataSource mDataSource = (DataSource)i.next();
         * if(mDataSource.getType() == DataSource.DR_FEDORA)
         * mDataSources.add(mDataSource);
         * }
         */
        return mDataSources;
        
    }
    
    public static void saveDataSourceViewer(){
        
        // if (dataSourceChanged){
        //    int choice = JOptionPane.showConfirmDialog(null,"Data Sources have been changed. Would you like to save them? ","Confirm Save",JOptionPane.YES_NO_CANCEL_OPTION);
        //if(choice == 0) {
        
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        Vector sDataSources = new Vector();
        int size = dataSourceList.getModel().getSize();
        int i;
        
        for (i = 0; i< size; i++){
            
            if (!(dataSourceList.getModel().getElementAt(i) instanceof String)) sDataSources.add((DataSource)dataSourceList.getModel().getElementAt(i));
            
        }
        
        SaveDataSourceViewer sViewer= new SaveDataSourceViewer(sDataSources);
        
        marshallMap(f,sViewer);
        
        
        //}
        //}
        
    }
    
    
    public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
        Marshaller marshaller = null;
        
        
        Mapping mapping = new Mapping();
        
        
        try {
            FileWriter writer = new FileWriter(file);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING_DEFAULT);
            marshaller.setMapping(mapping);
            
            
            marshaller.marshal(dataSourceViewer);
            
            writer.flush();
            writer.close();
            
        }
        catch (Exception e) {System.err.println("DRBrowser.marshallMap " + e);}
        
    }
    
    
    public  SaveDataSourceViewer unMarshallMap(File file) throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException{
        Unmarshaller unmarshaller = null;
        SaveDataSourceViewer sviewer = null;
        
        
        
        Mapping mapping = new Mapping();
        
        
        unmarshaller = new Unmarshaller();
        mapping.loadMapping(XML_MAPPING_DEFAULT);
        unmarshaller.setMapping(mapping);
        
        FileReader reader = new FileReader(file);
        
        sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
        
        
        reader.close();
        
        
        
        return sviewer;
    }
    
    
    
    
    
    
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
    
    
}
