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

import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class PrideConverterTest extends ItemsTestCase
{
    public PrideConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader =
            new InputStreamReader(getClass().getClassLoader().getResourceAsStream("pride1.xml"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        PrideConverter converter = new PrideConverter(itemWriter,
                                                          Model.getInstanceByName("genomic"));


        File tmpFile = File.createTempFile("PrideConverterTest", "tmp");
        FileWriter fw = new FileWriter(tmpFile);

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("fasta.fasta");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }

        fw.close();
        tmpFile.deleteOnExit();

        converter.setFastaPath(tmpFile.getAbsolutePath());

        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "/tmp/pride_item_test.xml");
    }
}
