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

package  edu.tufts.osidimpl.repository.fedora_2_0;
/**
 *
 * @author  akumar03
 */

import java.io.*;
import java.net.*;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;

public class FedoraUtils {
    
    /** Creates a new instance of FedoraUtils */
    public static final String SEPARATOR = ",";
    public static final String NOT_DEFINED = "Property not defined";
    
    private static java.util.Map prefsCache = new java.util.HashMap();
    
    public static java.util.Vector stringToVector(String str) {
        java.util.Vector vector = new java.util.Vector();
        java.util.StringTokenizer  st = new java.util.StringTokenizer(str,SEPARATOR);
        while(st.hasMoreTokens()){
            vector.add(st.nextToken());
        }
        return vector;
    }
    
    public static  String processId(String pid ) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(pid,":");
        String processString = "";
        while(st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }
    
    public static String getFedoraProperty(Repository repository,String pLookupKey)
    throws org.osid.repository.RepositoryException {
        try {
            return getPreferences(repository).get(pLookupKey, NOT_DEFINED);
        } catch (Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraUtils.getFedoraProperty: " + ex);
        }
    }
    
    public static Preferences getPreferences(Repository repository)
    throws java.io.FileNotFoundException, java.io.IOException, java.util.prefs.InvalidPreferencesFormatException {
        if(repository.getPrefernces() != null) {
            return repository.getPrefernces();
        } else {
            URL url = repository.getConfiguration();
            Preferences prefs = (Preferences) prefsCache.get(url);
            if (prefs != null)
                return prefs;
            //String filename = url.getFile().replaceAll("%20"," ");
            //prefs = Preferences.userRoot().node("/");
            Class clazz = new FedoraUtils().getClass();
            prefs = Preferences.userNodeForPackage(clazz);
            //System.out.println("*** " + clazz.getName() + ".getPreferences: loading & caching prefs from \"" + url + "\"");
            //InputStream stream = new BufferedInputStream(new FileInputStream(filename));
            InputStream stream = new BufferedInputStream(url.openStream());
            prefs.importPreferences(stream);
            prefsCache.put(url, prefs);
            stream.close();
            return prefs;
        }
    }
    
    public static String[] getFedoraPropertyArray(Repository repository,String pLookupKey)
    throws org.osid.repository.RepositoryException {
        String pValue = getFedoraProperty(repository,pLookupKey);
        return pValue.split(SEPARATOR);
    }
    
    public static String[] getAdvancedSearchFields(Repository repository)  throws org.osid.repository.RepositoryException{
        return getFedoraPropertyArray(repository,"fedora.search.advanced.fields");
    }
    public static String[] getAdvancedSearchOperators(Repository repository)  throws org.osid.repository.RepositoryException{
        return getFedoraPropertyArray(repository,"fedora.search.advanced.operators");
        
    }
    public static String getAdvancedSearchOperatorsActuals(Repository repository,String pOperator) throws org.osid.repository.RepositoryException{
        String[] pOperators =   getAdvancedSearchOperators(repository);
        String[] pOperatorsActuals = getFedoraPropertyArray(repository,"fedora.search.advanced.operators.actuals");
        String pValue = NOT_DEFINED;
        boolean flag = true;
        for(int i =0;i<pOperators.length && flag;i++) {
            if(pOperators[i].equalsIgnoreCase(pOperator)) {
                pValue = pOperatorsActuals[i];
                flag = false;
            }
        }
        return pValue;
    }
    
    
    
    
    public static String getSaveFileName(org.osid.shared.Id objectId,org.osid.shared.Id behaviorId,org.osid.shared.Id disseminationId) throws org.osid.OsidException {
        String saveFileName = processId(objectId.getIdString()+"-"+behaviorId.getIdString()+"-"+disseminationId.getIdString());
        return saveFileName;
    }
    
    
    public static AbstractAction getFedoraAction(org.osid.repository.Record record,org.osid.repository.Repository repository) throws org.osid.repository.RepositoryException {
        final Repository mRepository = (Repository)repository;
        final Record mRecord = (Record)record;
        
        try {
            AbstractAction fedoraAction = new AbstractAction(record.getId().getIdString()) {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                        //String fedoraUrl = mDR.getFedoraProperties().getProperty("url.fedora.get","http://vue-dl.tccs..tufts.edu:8080/fedora/get");
                        
                        // get part by iterating otherwise, we need the asset.                        
//                        String fedoraUrl = mRecord.getPart(new PID(getFedoraProperty(mRepository, "DisseminationURLInfoPartId"))).getValue().toString();
                        org.osid.shared.Id id = new PID(getFedoraProperty(mRepository, "DisseminationURLInfoPartId"));
                        org.osid.repository.PartIterator partIterator = mRecord.getParts();
                        while (partIterator.hasNextPart())
                        {
                            org.osid.repository.Part part = partIterator.nextPart();
//                            if (part.getId().isEqual(id))
                            {
                                String fedoraUrl = part.getValue().toString();
                                URL url = new URL(fedoraUrl);
                                URLConnection connection = url.openConnection();
                                System.out.println("FEDORA ACTION: Content-type:"+connection.getContentType()+" for url :"+fedoraUrl);                        
                                tufts.Util.openURL(fedoraUrl);
                                break;
                            }
                        }
                    } catch(Throwable t) {  }
                }
            };
            return fedoraAction;
        } catch(Throwable t) {
            throw new org.osid.repository.RepositoryException("FedoraUtils.getFedoraAction "+t.getMessage());
        }
    }

    private FedoraUtils() {}    
}