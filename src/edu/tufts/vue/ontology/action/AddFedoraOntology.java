
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */


/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.action;

import java.awt.event.ActionEvent;
import java.net.URL;

import edu.tufts.vue.ontology.ui.OntologyBrowser;
import edu.tufts.vue.ontology.ui.OntologyChooser2;
import edu.tufts.vue.ontology.ui.TypeList;

import tufts.vue.VueAction;
import tufts.vue.VueResources;


/*
 * AddFedoraOntology.java
 *
 * Created on November 5, 2007, 10:02 AM
 *
 * @author dhelle01
 */
public class AddFedoraOntology extends VueAction{
    
    private OntologyBrowser browser;
    
    public AddFedoraOntology(OntologyBrowser browser) 
    {
        setActionName("Add Fedora Ontology");
        this.browser = browser;
    }
            
    public void actionPerformed(ActionEvent e) 
    {
                TypeList list = new TypeList();
                URL ontURL = VueResources.getURL("fedora.ontology.rdf");
                URL cssURL = VueResources.getURL("fedora.ontology.css");
                tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),browser,w);

    }
    
}

