package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.web.ForwardParameters;
import org.intermine.web.SessionMethods;

/**
 * Action that creates a table of collection elements for display.
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public class CollectionDetailsAction extends Action
{
    private static int index = 0;
    
    /**
     * Create PagedTable for this collection, register it with an identifier and
     * redirect to results.do?table=identifier
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        Integer id = new Integer((String) request.getParameter("id"));
        String field = request.getParameter("field");
        String pageSize = request.getParameter("pageSize");

        Object o = os.getObjectById(id);

        Set cds = model.getClassDescriptorsForClass(o.getClass());

        CollectionDescriptor colDesc = null;
        
        Iterator iter = cds.iterator();

        while (iter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) iter.next();

            colDesc = (CollectionDescriptor) cd.getFieldDescriptorByName(field);

            if (colDesc != null) {
                break;
            }
        }

        ClassDescriptor collectionClass = colDesc.getReferencedClassDescriptor();

        Collection c = (Collection) TypeUtil.getFieldValue(o, field);
        PagedCollection pc = new PagedCollection(field, c, collectionClass);
        String identifier = "col" + index++;
        SessionMethods.setResultsTable(session, identifier, pc);
        
        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", pageSize)
                        .addParameter("trail", request.getParameter("trail")).forward();
    }
}
