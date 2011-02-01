package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbop.io.ExtensionFilenameFilter;
import org.intermine.dataconversion.DirectoryConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * 
 * @author Chen Yian
 */
public class MetazoanConverter extends DirectoryConverter {

	private static final String INTERACTION_TYPE = "Transcriptional regulation";

	private static final Logger LOG = Logger.getLogger(MetazoanConverter.class);

	private Map<String, String> expMap;

	private Map<String, Item> geneIdMap = new HashMap<String, Item>();
	private Map<String, Item> pubMedIdMap = new HashMap<String, Item>();
	private Item dataset;

	public MetazoanConverter(ItemWriter writer, Model model) {
		super(writer, model);
		dataset = creatDataSet();
		expMap = createExpMap();
	}

	private Map<String, String> createExpMap() {
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("Ex", "gene expression microarrays");
		ret.put("CC", "ChIP-chip");
		ret.put("C-DSL", "ChIP-DSL");
		ret.put("DamID", "DamID");
		ret.put("GO", "Gene Ontology database");
		return ret;
	}

	private Item creatDataSet() {
		Item dataSource = createItem("DataSource");
		dataSource.setAttribute("name", "Amadeus");

		Item dataSet = createItem("DataSet");
		dataSet.setAttribute("title", "Metazoan compendium");
		dataSet.setReference("dataSource", dataSource);
		dataSet.setAttribute("url", "http://acgt.cs.tau.ac.il/amadeus");
		dataSet.setAttribute("description", "Compendium of metazoan TF and miRNA target sets");

		try {
			store(dataSource);
			store(dataSet);
		} catch (ObjectStoreException e) {
			LOG.error("failed to store DataSource/DataSet of Amadeus");
		}

		return dataSet;
	}

	@Override
	public void process(File dataDir) throws Exception {
		File[] files = dataDir.listFiles(new ExtensionFilenameFilter("geneid.txt"));

		for (File file : files) {
			// Name the file as following format:
			// [Ensembl ID]_[Source]_[PubMed ID].ensid.txt (deprecated)
			// [Entrez Gene ID]_[Source]_[PubMed ID].geneid.txt
			LOG.debug("processing file: " + file.getName());

			String[] cols = file.getName().split("_");
			Item sourceGene = getGeneByNcbiGeneNumber(cols[0]);
			String pubmedId = cols[2].replaceAll("\\.geneid\\.txt", "");
			Item publication = getPublication(pubmedId);

			Item intExp = createItem("ProteinDNAExperiment");
			intExp.setReference("publication", publication);
			intExp.setAttribute("name", cols[1]);
			intExp.setAttribute("description", expMap.get(cols[1]));

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String ncbiGeneId;
			while ((ncbiGeneId = reader.readLine()) != null) {
				if (ncbiGeneId.equals(cols[0])) {
					// create Interaction
					createInteraction(sourceGene, sourceGene, "source&target", intExp);

				} else {
					// create Interaction for source
					Item targetGene = getGeneByNcbiGeneNumber(ncbiGeneId);
					createInteraction(sourceGene, targetGene, "source", intExp);

					// create Interaction for target
					createInteraction(targetGene, sourceGene, "target", intExp);
				}
			}
			reader.close();

			store(intExp);
		}

		// store all created genes
		store(geneIdMap.values());
	}

	private Item createInteraction(Item master, Item slave, String role, Item interactionExperiment)
			throws ObjectStoreException {
		Item ret = createItem("ProteinDNAInteraction");
		ret.setReference("gene", master);
		ret.setReference("interactWith", slave);

		ret.setAttribute("interactionType", INTERACTION_TYPE);
		ret.addToCollection("dataSets", dataset);
		ret.setAttribute("name", String.format("AMADEUS_G%s_G%s", master.getAttribute(
				"ncbiGeneNumber").getValue(), slave.getAttribute("ncbiGeneNumber").getValue()));
		ret.setAttribute("role", role);
		ret.setReference("experiment", interactionExperiment);
		store(ret);
		master.addToCollection("proteinDNAinteractions", ret);
		interactionExperiment.addToCollection("interactions", ret);
		return ret;
	}

	private Item getPublication(String pubMedId) throws ObjectStoreException {
		if (pubMedIdMap.containsKey(pubMedId)) {
			return pubMedIdMap.get(pubMedId);
		} else {
			Item pub = createItem("Publication");
			pub.setAttribute("pubMedId", pubMedId);
			store(pub);
			LOG.info(String.format("Publication id:%s was created.", pubMedId));
			pubMedIdMap.put(pubMedId, pub);
			return pub;
		}
	}

	private Item getGeneByNcbiGeneNumber(String ncbiGeneId) throws ObjectStoreException {
		if (geneIdMap.containsKey(ncbiGeneId)) {
			return geneIdMap.get(ncbiGeneId);
		} else {
			Item gene = createItem("Gene");
			gene.setAttribute("ncbiGeneNumber", ncbiGeneId);
			geneIdMap.put(ncbiGeneId, gene);
			return gene;
		}
	}
}
