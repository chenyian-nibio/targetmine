package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * 
 * @author chenyian
 */
public class Protein2iprConverter extends BioFileConverter {
	private static final Logger LOG = Logger.getLogger(Protein2iprConverter.class);
	//
	private static final String DATASET_TITLE = "InterPro data set";
	private static final String DATA_SOURCE_NAME = "InterPro";

	private Collection<Integer> taxonIds = new ArrayList<Integer>();

	private Set<String> proteinIds = new HashSet<String>();

	// intermine item map
	private Map<String, String> proteinMap = new HashMap<String, String>();
	private Map<String, String> proteinDomainMap = new HashMap<String, String>();

	public void setOrganisms(String taxonIds) {
		String[] taxonStringIds = StringUtils.split(taxonIds, " ");
		for (String string : taxonStringIds) {
			this.taxonIds.add(Integer.valueOf(string));
		}
		LOG.info("Setting list of organisms to " + this.taxonIds);
	}

	/**
	 * Constructor
	 * 
	 * @param writer
	 *            the ItemWriter used to handle the resultant items
	 * @param model
	 *            the Model
	 */
	public Protein2iprConverter(ItemWriter writer, Model model) {
		super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
	}

	/**
	 * 
	 * 
	 * {@inheritDoc}
	 */
	public void process(Reader reader) throws Exception {
		getProteinIds();
		LOG.info("Found " + proteinIds.size() + " protein ids.");
		
		Iterator<String[]> iterator = FormattedTextParser.parseTabDelimitedReader(reader);
		int count = 0;
		int skipped = 0;
		while (iterator.hasNext()) {
			String[] cols = iterator.next();
			if (proteinIds.contains(cols[0])) {
				Item item = createItem("ProteinDomainRegion");

				item.setAttribute("start", cols[4]);
				item.setAttribute("end", cols[5]);
				item.setAttribute("originalId", cols[3]);
				item.setAttribute("originalDb", getDatabaseName(cols[3]));
				item.setReference("proteinDomain", getProteinDomain(cols[1]));
				item.setReference("protein", getProtein(cols[0]));

				store(item);
				count++;
			} else {
				skipped++;
			}
		}
		LOG.info("Number of processed lines: " + count);
		LOG.info("Number of skipped lines: " + skipped);
	}

	private String getProtein(String identifier) throws ObjectStoreException {
		String ret = proteinMap.get(identifier);
		if (ret == null) {
			Item item = createItem("Protein");
			item.setAttribute("primaryAccession", identifier);
			ret = item.getIdentifier();
			store(item);
			proteinMap.put(identifier, ret);
		}
		return ret;
	}

	private String getProteinDomain(String identifier) throws ObjectStoreException {
		String ret = proteinDomainMap.get(identifier);
		if (ret == null) {
			Item item = createItem("ProteinDomain");
			item.setAttribute("primaryIdentifier", identifier);
			ret = item.getIdentifier();
			store(item);
			proteinDomainMap.put(identifier, ret);
		}
		return ret;
	}

	private String getDatabaseName(String dbId) {
		String dbName = null;
		if (dbId.startsWith("PF")) {
			dbName = "Pfam";
		} else if (dbId.startsWith("SM")) {
			dbName = "SMART";
		} else if (dbId.startsWith("SSF")) {
			dbName = "SUPERFAMILY";
		} else if (dbId.startsWith("PS")) {
			dbName = "PROSITE";
		} else if (dbId.startsWith("PR")) {
			dbName = "PRINTS";
		} else if (dbId.startsWith("PTHR")) {
			dbName = "PANTHER";
		} else if (dbId.startsWith("G3DSA")) {
			dbName = "Gene3D";
		} else if (dbId.startsWith("TIGR")) {
			dbName = "TIGRFAMs";
		} else if (dbId.startsWith("PD")) {
			dbName = "ProDom";
		} else if (dbId.startsWith("PIRSF")) {
			dbName = "PIRSF";
		} else if (dbId.startsWith("MF_")) {
			dbName = "HAMAP";
		} else {
			throw new RuntimeException("Unknown DB found. ID: " + dbId);
		}

		return dbName;
	}

	private String osAlias = null;

	public void setOsAlias(String osAlias) {
		this.osAlias = osAlias;
	}

	@SuppressWarnings("unchecked")
	private void getProteinIds() throws Exception {

		Query q = new Query();
		QueryClass qcProtein = new QueryClass(Protein.class);
		QueryClass qcOrganism = new QueryClass(Organism.class);

		QueryField qfPrimaryAcc = new QueryField(qcProtein, "primaryAccession");
		QueryField qfOrganismTaxonId = new QueryField(qcOrganism, "taxonId");

		q.addFrom(qcProtein);
		q.addFrom(qcOrganism);
		q.addToSelect(qfPrimaryAcc);

		ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

		// organism in our list
		cs.addConstraint(new BagConstraint(qfOrganismTaxonId, ConstraintOp.IN, taxonIds));

		// protein.organism = organism
		QueryObjectReference qor = new QueryObjectReference(qcProtein, "organism");
		cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

		q.setConstraint(cs);

		ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);

		Results results = os.execute(q);
		Iterator<Object> iterator = results.iterator();
		while (iterator.hasNext()) {
			ResultsRow<String> rr = (ResultsRow<String>) iterator.next();
			proteinIds.add(rr.get(0));
		}
	}

}