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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.DataConverter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 * Processor of file with information about genes. Format of file:
 * <tt>
 * tax_id GeneID Symbol LocusTag Synonyms dbXrefs chromosome map_location description
 * type_of_gene Symbol_from_nomenclature_authority Full_name_from_nomenclature_authority
 * Nomenclature_status Other_designations Modification_date (tab is used as a separator,
 * pound sign - start of a comment)
 * </tt>
 * @author Jakub Kulaviak
 **/
public class GenesFileProcessor
{

    private BufferedReader infoReader;
    private int lineCounter = 0;
    private Map<String, Item> genes = new HashMap<String, Item>();
    private String lastLine = null;
    private DataConverter converter;
    private Integer checkOrganismId = null;
    private Set<Integer> alreadyProcessedGenes = new  HashSet<Integer>();
    private Set<String> genesToRemove = new TreeSet<String>();
    private IdResolver resolver;
    private static final Logger LOG = Logger.getLogger(GenesFileProcessor.class);
    private String datasetRefId;

    /**
     * Constructor.
     * @param fileReader file reader, this class is not responsible for closing fileReader
     * @param converter associated converter that is used for creating and saving items
     * @param datasetRefId reference to dataset object for the gene
     * @param resolverFactory the FlyBase id resolver factory
     */
    public GenesFileProcessor(Reader fileReader, DataConverter converter, String datasetRefId,
                              IdResolverFactory resolverFactory) {
        // converter is needed  for creating items method
        // all converters must used one central converter for creating items because
        // to be sure, that created items will have unique id
        this.converter = converter;
        this.datasetRefId = datasetRefId;
        initReader(fileReader);
        resolver = resolverFactory.getIdResolver(false);
    }

    private void initReader(Reader fileReader) {
         infoReader = new BufferedReader(fileReader);
    }

    /**
     *
     * @param geneToPub map between gene and list of publication that mentions this gene
     * @param orgToProcessId organism to be processed id
     * @param orgToProcess organism to be processed
     * @throws IOException when error happens during reading from file
     */
    public void processGenes(Map<Integer, List<String>> geneToPub, Integer orgToProcessId,
                             Item orgToProcess) throws IOException {
        String line;
        // use taxonID to get correct type of data where available
        while ((line = getLine()) != null) {
            lineCounter++;
            line = line.trim();
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts.length < 6) {
                throw new GenesProcessorException("Invalid line - line " + lineCounter
                        + ". There are " + parts.length + " bits but there should be more than 6");
            }
            Integer organismId, ncbiGeneId;
            try {
                organismId = new Integer(parts[0].trim());
                ncbiGeneId = new Integer(parts[1].trim());
            } catch (NumberFormatException ex) {
                throw new GenesProcessorException("Invalid identifiers at line " + line);
            }
            checkFileIsSorted(organismId);
            //String identifier = parts[3].trim();
            String pubMedId = parts[5].trim();
            if (orgToProcessId.intValue() == organismId.intValue()) {
                processGeneInfo(ncbiGeneId, organismId, pubMedId, geneToPub.get(ncbiGeneId),
                                orgToProcess);
                geneToPub.remove(ncbiGeneId);
            } else if (organismId.intValue() > orgToProcessId.intValue()) {
                lastLine = line;
                storeGenes();
                checkGenesProcessed(geneToPub);
                return;
            } else {
                continue;
            }
        }
        storeGenes();
        checkGenesProcessed(geneToPub);
    }

    private void checkGenesProcessed(Map<Integer, List<String>> geneToPub) {
        if (geneToPub.size() != 0) {
            throw new GenesProcessorException("These " + geneToPub.size() + " genes were in the "
                                              + "PubMed2Gene file but not in the gene info file: "
                                              + formatGeneNames(geneToPub.keySet()));
        }
    }

    private void checkFileIsSorted(Integer organismId) {
        if (checkOrganismId != null) {
            if (organismId.intValue() < checkOrganismId.intValue()) {
                throw new GenesProcessorException("This file processor expects that "
                            + "file is sorted according to the organism id else the "
                            + "behaviour is undefined.");
            }
        }
        checkOrganismId = organismId;
    }

    private String formatGeneNames(Set<Integer> keySet) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : keySet) {
            sb.append(id + ", ");
        }
        return sb.toString();
    }

    private String getLine() throws IOException {
        if (lastLine != null) {
            String tmp = lastLine;
            lastLine = null;
            return tmp;
        }
        return infoReader.readLine();
    }

    private void storeGenes()  {
        for (String id : genesToRemove) {
            genes.remove(id);
        }
        try {
            List<Item> gs = new ArrayList<Item>();
            for (String id : genes.keySet()) {
                gs.add(genes.get(id));
            }
            store(gs);
        } catch (ObjectStoreException e) {
            throw new GenesProcessorException(e);
        }
        genes = new HashMap<String, Item>();
    }

    private void store(List<Item> genes2) throws ObjectStoreException {
        converter.store(genes2);
    }

    private void processGeneInfo(Integer ncbiGeneId, Integer organismId, String primaryIdentifier,
                                 List<String> publications, Item organism) {

        String primIdentifier = primaryIdentifier;

        // If gene was already mentioned in gene info file then is skipped
        if (alreadyProcessedGenes.contains(ncbiGeneId)) {
            return;
        }
        // If there is a gene in gene information file that doesn't have
        // any publication then the gene is skipped
        // if there isn't primary identifier gene is skipped
        if (publications != null && !primIdentifier.equals("-")) {
            primIdentifier = removeDatabasePrefix(primIdentifier);
            if (!isValidPrimIdentifier(primIdentifier)) {
                return;
            }

            if (isDrosophilaMelanogaster(organismId.toString()) && resolver != null) {
                primIdentifier = resolvePrimIdentifier(organismId.toString(), primIdentifier);
            }

            if (primIdentifier == null) {
                LOG.warn("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                        + primaryIdentifier + ". Number of matched ids: "
                        + resolver.countResolutions(organismId.toString(), primIdentifier));
                return;
            }
            Item gene = createGene(ncbiGeneId, primIdentifier, organism);
            for (String writerPubId : publications) {
                gene.addToCollection("publications", writerPubId);
            }
            // checks gene duplicates - if there are two or more same genes with
            // the same primIdentifier but different ncbi gene id then all these genes are removed
            if (genes.get(primIdentifier) == null) {
                genes.put(primIdentifier, gene);
            } else {
                genesToRemove.add(primIdentifier);
            }
            alreadyProcessedGenes.add(ncbiGeneId);
        }
    }

    private boolean isValidPrimIdentifier(String primIdentifier) {
        return !primIdentifier.contains("|");
    }

    private String resolvePrimIdentifier(String taxonId, String primIdentifier) {
        int resCount = resolver.countResolutions(taxonId, primIdentifier);
        if (resCount != 1) {
            return null;
        }
        return resolver.resolveId(taxonId, primIdentifier).iterator().next();
    }

    private boolean isDrosophilaMelanogaster(String taxonId) {
        return taxonId.equals("7227");
    }

    private Item createGene(Integer ncbiGeneId, String primaryIdentifier, Item organism) {
        Item gene = createItem("Gene");
        gene.setAttribute("ncbiGeneNumber", ncbiGeneId.toString());
        gene.setAttribute("primaryIdentifier", primaryIdentifier);
        gene.setReference("organism", organism);
        gene.setCollection("dataSets", new ArrayList(Collections.singleton(datasetRefId)));
        return gene;
    }

    private String removeDatabasePrefix(String id) {
        String dbId = id;
        if (dbId.toUpperCase().startsWith("SGD:")) {
            dbId = dbId.substring(4);
        } else if (dbId.toUpperCase().startsWith("WORMBASE:")) {
            dbId = dbId.substring(9);
        } else if (dbId.toUpperCase().startsWith("FLYBASE:")) {
            dbId = dbId.substring(8);
        } else if (dbId.toUpperCase().startsWith("VECTORBASE:")) {
            dbId = dbId.substring(11);
        }
        return dbId;
    }

    private Item createItem(String className) {
        return converter.createItem(className);
    }
}
