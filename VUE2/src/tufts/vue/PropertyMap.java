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
  * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
  * Tufts University. All rights reserved.</p>
  *
  * -----------------------------------------------------------------------------
  */


package tufts.vue;

import java.util.*;

/**
 * A general HashMap for storing property values: e.g., meta-data.
 *
 * @version $Revision: 1.10 $ / $Date: 2006-07-28 00:07:21 $ / $Author: mike $
 */

public class PropertyMap extends java.util.HashMap
{
    public interface Listener {
        void propertyMapChanged(PropertyMap p);
    }

    private SortedMapModel mTableModel;
    private boolean mHoldingChanges = false;
    private int mChanges;
    private List listeners;

    public PropertyMap() {}

    public synchronized Object put(Object k, Object v) {

        // TODO: we want to *preserve* case for display, but not differentiate based on it...
        //if (k instanceof String) k = ((String)k).toLowerCase();

        Object prior = super.put(k, v);

        // todo: this a bit overkill: could have a higher level
        // trigger for this, instead of triggering any table listeners
        // every time.  Our hold/release deals with specific batch
        // loads, which is good enough for now.
        
        if (mHoldingChanges)
            mChanges++;
        else if (mTableModel != null)
            mTableModel.reload();

        return prior;

    }
    public synchronized Object get(Object k) {
        return super.get(k);
    }

    public String getProperty(Object key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }
    
    /** Set the value for the given key, overwriting any existing value. */
    public void setProperty(String key, String value) {
        put(key, value);
    }

    /**
     * Add a property with the given key.  If a key already exists
     * with this name, the key will be modified with an index.
     * @return - the key actually created
     */
    public synchronized String addProperty(final String desiredKey, Object value) {
        String key = desiredKey;
        int index = 10;
        while (containsKey(key))
            key = desiredKey + "." + index++;
        put(key, value);
        return key;
    }
    

    /** No listeners will be updated until releaseChanges is called.  Multiple
     * overlapping holds are okay. */
    public synchronized void holdChanges() {
        if (DEBUG.RESOURCE) out("holding changes");
        mHoldingChanges = true;
    }

    /**
     * If any changes made since holdChanges, listeners will be notified.
     * It is crucial this is called at some point after holdChanges,
     * or listeners may never get updated. Tho ideally only one call to
     * this per holdChanges is made, Extra calls to this are okay 
     * -- it will at worst degrade update performance with rapid updates.
     */
    public synchronized void releaseChanges() {
        mHoldingChanges = false;
        if (mChanges > 0 && mTableModel != null) {
            if (DEBUG.RESOURCE) out("releasing changes " + mChanges);
            mTableModel.reload();
        }
        if (DEBUG.RESOURCE) out("released changes " + mChanges);
        mChanges = 0;
    }

    public synchronized java.util.Properties asProperties() {
        java.util.Properties props = new java.util.Properties();
        // todo: this not totally safe: values may not all be strings
        props.putAll(this);
        return props;
    }

    public synchronized javax.swing.table.TableModel getTableModel() {
        if (mTableModel == null)
            mTableModel = new SortedMapModel();
        return mTableModel;

    }

    public synchronized void addListener(Listener l) {
        if (listeners == null)
            listeners = new java.util.ArrayList();
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l) {
        if (listeners != null)
            listeners.remove(l);
    }
    public synchronized void notifyListeners() {
        if (listeners != null) {
            Iterator i = listeners.iterator();
            while (i.hasNext())
                ((Listener)i.next()).propertyMapChanged(this);
        }
    }

    private static class Entry implements Comparable {
        final String key;
        final Object value;
        final boolean priority;
        
        Entry(Map.Entry e, boolean priority) {
            this.key = tufts.Util.upperCaseWords((String) e.getKey());
            this.value = e.getValue();
            this.priority = priority;
        }

        Entry(Map.Entry e) {
            this(e, false);
        }
        public int compareTo(Object o) {
            if (priority)
                return Short.MIN_VALUE;
            else if (((Entry)o).priority)
                return Short.MAX_VALUE;
            else
                return key.compareTo(((Entry)o).key);
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    private void out(Object o) {
        VUE.Log.debug("PropertyMap: " + (o==null?"null":o.toString()));
    }
    

    // TODO: move this out to viewer
    
    private class SortedMapModel extends javax.swing.table.AbstractTableModel {

        private Entry[] mEntries;
        
        SortedMapModel() {
            if (DEBUG.RESOURCE) out("new SortedMapModel");
            reload();
        }

        // make sure there is a sync on the HashMap before this is called
        private void reload() {
            mEntries = new Entry[size()];
            if (DEBUG.RESOURCE) out("SortedMapModel: reload " + mEntries.length + " items");
            Iterator i = entrySet().iterator();
            int ei = 0;
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                boolean priority = false;
                if ("title".equalsIgnoreCase((String)e.getKey()))
                    priority = true;                
                if ("name".equalsIgnoreCase((String)e.getKey()))
                    priority = true;
                mEntries[ei++] = new Entry(e, priority);
            }
            
            Arrays.sort(mEntries);
            
            /*
            mEntries = (Map.Entry[]) PropertyMap.this.entrySet().toArray(new Map.Entry[size()]);
            Arrays.sort(mEntries, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String k1 = (String) ((Map.Entry)o1).getKey();
                        String k2 = (String) ((Map.Entry)o2).getKey();
                        return k1.compareTo(k2);
                    }});
            */
            if (DEBUG.RESOURCE) {
                out("loaded " + mEntries.length + " entries");
                if (DEBUG.META)
                    out("model loaded " + Arrays.asList(mEntries));
            }
            fireTableDataChanged();
            notifyListeners();
        }
        
        public int getRowCount() {
            return mEntries.length;
        }
        
        public Object getValueAt(int row, int col) {
            if (col == 0)
                return mEntries[row].key;
            else
                return mEntries[row].value;
        }

        public int getColumnCount() { return 2; }

        public String getColumnName(int col) {
            //return col == 0 ? "Field" : "Value";
            return null;
        }

        public Class getColumnClass(int colIndex) {
            return colIndex == 0 ? String.class : Object.class;
        }

        
        
    }
}