package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

/**
 * Tests for the OrganismRepositoryclass.
 * @author Kim Rutherford
 */
public class OrganismRepositoryTest extends TestCase
{
    /*
     * Test method for OrganismRepository.getOrganismRepository().
     */
    public void testGetOrganismRepository() {
        OrganismRepository organismRepository = OrganismRepository.getOrganismRepository();
        OrganismData drosOrganismData = organismRepository.getOrganismDataByTaxon(7227);
        assertEquals(7227, drosOrganismData.getTaxonId());
        assertEquals("melanogaster", drosOrganismData.getSpecies());
        assertEquals("Drosophila", drosOrganismData.getGenus());
        assertEquals("Dmel", drosOrganismData.getAbbreviation());

        OrganismData celegansData = organismRepository.getOrganismDataByAbbreviation("C.elegans");
        assertEquals(6239, celegansData.getTaxonId());
        assertEquals("elegans", celegansData.getSpecies());
        assertEquals("Caenorhabditis", celegansData.getGenus());
        assertEquals("C.elegans", celegansData.getAbbreviation());
    }

    // fetching by abbreviation should be case insensitive
    public void testGetOrganismDataByAbbreviation() {
        OrganismRepository organismRepository = OrganismRepository.getOrganismRepository();
        assertNotNull(organismRepository.getOrganismDataByAbbreviation("Dmel"));
        assertNotNull(organismRepository.getOrganismDataByAbbreviation("dmel"));
        assertNull(organismRepository.getOrganismDataByAbbreviation("monkey"));
    }

    public void testGetOrganismDataByGenusSpecies() {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        assertNotNull(or.getOrganismDataByGenusSpecies("Drosophila", "melanogaster"));
        assertNull(or.getOrganismDataByGenusSpecies("Sphenodon", "punctatus"));
    }
    
    public void testGetOrganismDataByFullName() {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        assertNotNull(or.getOrganismDataByFullName("Drosophila melanogaster"));
        assertNotNull(or.getOrganismDataByFullName("Drosophila pseudoobscura pseudoobscura"));
    }
}
