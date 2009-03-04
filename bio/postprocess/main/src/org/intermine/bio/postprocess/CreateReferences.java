package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.model.bio.CDS;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.ChromosomeBand;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.FivePrimeUTR;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.ThreePrimeUTR;
import org.intermine.model.bio.Transcript;
import org.intermine.model.bio.UTR;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.DatabaseUtil;

/**
 * Fill in additional references/collections in genomic model
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateReferences
{
    private static final Logger LOG = Logger.getLogger(CreateReferences.class);

    protected ObjectStoreWriter osw;
    private Model model;

    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public CreateReferences(ObjectStoreWriter osw) {
        this.osw = osw;
        this.model = Model.getInstanceByName("genomic");
    }

    /**
     * Fill in missing references/collections in model by querying relations
     * @throws Exception if anything goes wrong
     */
    public void insertReferences() throws Exception {

        LOG.info("insertReferences stage 1");
        // fill in collections on Chromosome
        insertCollectionField(Gene.class, "objects", Location.class, "object",
                              Chromosome.class, "genes", false);
        insertCollectionField(Transcript.class, "objects", Location.class, "object",
                              Chromosome.class, "transcripts", false);
        insertCollectionField(Exon.class, "objects", Location.class, "object",
                              Chromosome.class, "exons", false);
        insertCollectionField(ChromosomeBand.class, "objects", Location.class, "object",
                              Chromosome.class, "chromosomeBands", false);

        LOG.info("insertReferences stage 2");
        // Exon.gene / Gene.exons
        insertReferenceField(Gene.class, "transcripts", Transcript.class, "exons",
                             Exon.class, "gene");
        LOG.info("insertReferences stage 3");
        // UTR.gene / Gene.UTRs
        insertReferenceField(Gene.class, "transcripts", MRNA.class, "UTRs",
                             UTR.class, "gene");

        LOG.info("insertReferences stage 4");
        // CDS.gene / Gene.CDSs
        insertReferenceField(Gene.class, "transcripts", MRNA.class, "CDSs",
                             CDS.class, "gene");

        LOG.info("insertReferences stage 5");
//        insertGeneAnnotationReferences();
    }


    /**
     * Add a reference to and object of type X in objects of type Y by using a connecting class.
     * Eg. Add a reference to Gene objects in Exon by examining the Transcript objects in the
     * transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene FROM Gene AS gene, Transcript AS transcript, Exon AS exon WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.exons CONTAINS exon) ORDER BY gene
     * and then set exon.gene
     *
     * in overview we are doing:
     * BioEntity1 -&gt; BioEntity2 -&gt; BioEntity3   ==&gt;   BioEntitiy1 -&gt; BioEntity3
     * @param sourceClass the first class in the query
     * @param sourceClassFieldName the field in the sourceClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by sourceClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * destinationClass
     * @param destinationClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the reference field in the destinationClass - the
     * collection to create/set
     * @throws Exception if anything goes wrong
     */
    protected void insertReferenceField(Class sourceClass, String sourceClassFieldName,
                                        Class connectingClass, String connectingClassFieldName,
                                        Class destinationClass, String createFieldName)
        throws Exception {
        LOG.info("Beginning insertReferences("
                 + sourceClass.getName() + ", "
                 + sourceClassFieldName + ", "
                 + connectingClass.getName() + ", "
                 + connectingClassFieldName + ","
                 + destinationClass.getName() + ", "
                 + createFieldName + ")");
        long startTime = System.currentTimeMillis();

        Iterator resIter =
            PostProcessUtil.findConnectingClasses(osw.getObjectStore(),
                                          sourceClass, sourceClassFieldName,
                                          connectingClass, connectingClassFieldName,
                                          destinationClass, true);

        // results will be sourceClass ; destClass (ordered by sourceClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisSourceObject = (InterMineObject) rr.get(0);
            InterMineObject thisDestObject = (InterMineObject) rr.get(1);

            try {
                // clone so we don't change the ObjectStore cache
                InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(thisDestObject);
                tempObject.setFieldValue(createFieldName, thisSourceObject);
                count++;
                if (count % 10000 == 0) {
                    LOG.info("Created " + count + " references in " + destinationClass.getName()
                             + " to " + sourceClass.getName()
                             + " via " + connectingClass.getName());
                }
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + thisDestObject.getId()
                          + " has no " + createFieldName + " field");
            }
        }

        LOG.info("Finished: created " + count + " references in " + destinationClass.getName()
                 + " to " + sourceClass.getName() + " via " + connectingClass.getName()
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(destinationClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }


    /**
     * Add a collection of objects of type X to objects of type Y by using a connecting class.
     * Eg. Add a collection of Protein objects to Gene by examining the Transcript objects in the
     * transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene FROM Gene AS gene, Transcript AS transcript, Protein AS protein WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.protein CONTAINS protein)
     *   ORDER BY gene
     * and then set protected gene.protein (if created
     * BioEntity1 -&gt; BioEntity2 -&gt; BioEntity3   ==&gt;   BioEntity1 -&gt; BioEntity3
     * @param firstClass the first class in the query
     * @param firstClassFieldName the field in the firstClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by firstClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * secondClass
     * @param secondClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the collection field in the secondClass - the
     * collection to create/set
     * @param createInFirstClass if true create the new collection field in firstClass, otherwise
     * create in secondClass
     * @throws Exception if anything goes wrong
     */
    protected void insertCollectionField(Class firstClass, String firstClassFieldName,
                                         Class connectingClass, String connectingClassFieldName,
                                         Class secondClass, String createFieldName,
                                         boolean createInFirstClass)
        throws Exception {
        InterMineObject lastDestObject = null;
        Set newCollection = new HashSet();

        LOG.info("Beginning insertCollectionField("
                 + firstClass.getName() + ", "
                 + firstClassFieldName + ", "
                 + connectingClass.getName() + ", "
                 + connectingClassFieldName + ","
                 + secondClass.getName() + ", "
                 + createFieldName + ", "
                 + createInFirstClass + ")");
        long startTime = System.currentTimeMillis();

        // if this is a many to many collection we can use ObjectStore.addToCollection which will
        // write directly to the database.
        boolean manyToMany = false;
        ClassDescriptor destCld;
        if (createInFirstClass) {
            destCld = model.getClassDescriptorByName(firstClass.getName());
        } else {
            destCld = model.getClassDescriptorByName(secondClass.getName());
        }
        CollectionDescriptor col = destCld.getCollectionDescriptorByName(createFieldName);
        if (col.relationType() == CollectionDescriptor.M_N_RELATION) {
            manyToMany = true;
        }
        
        Iterator resIter =
            PostProcessUtil.findConnectingClasses(osw.getObjectStore(),
                                          firstClass, firstClassFieldName,
                                          connectingClass, connectingClassFieldName,
                                          secondClass, createInFirstClass);

        // results will be firstClass ; destClass (ordered by firstClass)
        osw.beginTransaction();
        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisSourceObject;
            InterMineObject thisDestObject;

            if (createInFirstClass) {
                thisDestObject = (InterMineObject) rr.get(0);
                thisSourceObject = (InterMineObject) rr.get(1);
            } else {
                thisDestObject = (InterMineObject) rr.get(1);
                thisSourceObject = (InterMineObject) rr.get(0);
            }            
            
            if (!manyToMany && (lastDestObject == null
                || !thisDestObject.getId().equals(lastDestObject.getId()))) {

                if (lastDestObject != null) {
                    try {
                        InterMineObject tempObject =
                            PostProcessUtil.cloneInterMineObject(lastDestObject);
                        Set oldCollection = (Set) tempObject.getFieldValue(createFieldName);
                        newCollection.addAll(oldCollection);
                        tempObject.setFieldValue(createFieldName, newCollection);
                        count += newCollection.size();
                        osw.store(tempObject);
                   } catch (IllegalAccessException e) {
                        LOG.error("Object with ID " + thisDestObject.getId()
                                  + " has no " + createFieldName + " field", e);
                    }
                }

                newCollection = new HashSet();
            }

            if (manyToMany) {
                osw.addToCollection(thisDestObject.getId(), destCld.getType(),
                        createFieldName, thisSourceObject.getId());
            } else {
                newCollection.add(thisSourceObject);
            }
            
            lastDestObject = thisDestObject;
        }

        if (!manyToMany && lastDestObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastDestObject);
            tempObject.setFieldValue(createFieldName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }
        LOG.info("Finished: created " + count + " references in " + secondClass.getName() + " to "
                 + firstClass.getName() + " via " + connectingClass.getName()
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(secondClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }



    /**
     * Read the UTRs collection of MRNA then set the fivePrimeUTR and threePrimeUTR fields with the
     * corresponding UTRs.
     * @throws Exception if anything goes wrong
     */
    public void createUtrRefs() throws Exception {
        long startTime = System.currentTimeMillis();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcMRNA = new QueryClass(MRNA.class);
        q.addFrom(qcMRNA);
        q.addToSelect(qcMRNA);
        q.addToOrderBy(qcMRNA);

        QueryClass qcUTR = new QueryClass(UTR.class);
        q.addFrom(qcUTR);
        q.addToSelect(qcUTR);
        q.addToOrderBy(qcUTR);

        QueryCollectionReference mrnaUtrsRef =
            new QueryCollectionReference(qcMRNA, "UTRs");
        ContainsConstraint mrnaUtrsConstraint =
            new ContainsConstraint(mrnaUtrsRef, ConstraintOp.CONTAINS, qcUTR);

        QueryObjectReference fivePrimeRef = new QueryObjectReference(qcMRNA, "fivePrimeUTR");
        ContainsConstraint fivePrimeNullConstraint =
            new ContainsConstraint(fivePrimeRef, ConstraintOp.IS_NULL);
        QueryObjectReference threePrimeRef = new QueryObjectReference(qcMRNA, "threePrimeUTR");
        ContainsConstraint threePrimeNullConstraint =
            new ContainsConstraint(threePrimeRef, ConstraintOp.IS_NULL);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(mrnaUtrsConstraint);
        cs.addConstraint(fivePrimeNullConstraint);
        cs.addConstraint(threePrimeNullConstraint);

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                                                   .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 500, true, true, true);

        int count = 0;
        MRNA lastMRNA = null;

        FivePrimeUTR fivePrimeUTR = null;
        ThreePrimeUTR threePrimeUTR = null;

        osw.beginTransaction();

        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            MRNA mrna = (MRNA) rr.get(0);
            UTR utr = (UTR) rr.get(1);

            if (lastMRNA != null && !mrna.getId().equals(lastMRNA.getId())) {
                // clone so we don't change the ObjectStore cache
                MRNA tempMRNA = (MRNA) PostProcessUtil.cloneInterMineObject(lastMRNA);
                if (fivePrimeUTR != null) {
                    tempMRNA.setFieldValue("fivePrimeUTR", fivePrimeUTR);
                    fivePrimeUTR = null;
                }
                if (threePrimeUTR != null) {
                    tempMRNA.setFieldValue("threePrimeUTR", threePrimeUTR);
                    threePrimeUTR = null;
                }
                osw.store(tempMRNA);
                count++;
            }

            if (utr instanceof FivePrimeUTR) {
                fivePrimeUTR = (FivePrimeUTR) utr;
            } else {
                threePrimeUTR = (ThreePrimeUTR) utr;
            }

            lastMRNA = mrna;
        }

        if (lastMRNA != null) {
            // clone so we don't change the ObjectStore cache
            MRNA tempMRNA = (MRNA) PostProcessUtil.cloneInterMineObject(lastMRNA);
            tempMRNA.setFieldValue("fivePrimeUTR", fivePrimeUTR);
            tempMRNA.setFieldValue("threePrimeUTR", threePrimeUTR);
            osw.store(tempMRNA);
            count++;
        }
        LOG.info("Stored MRNA " + count + " times (" + count * 2 + " fields set)"
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();


        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(MRNA.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }
}
