package org.flymine.modelproduction.xml;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.flymine.metadata.*;

/**
 * DefaultHandler extension to support parsing of metadata XML
 *
 * @author Mark Woodbridge
 */
public class ModelParser
{
    protected ModelHandler handler = new ModelHandler();

    /**
     * Parse the metadata xml file
     * @param f the file to parse
     * @throws Exception if an error occuring during parsing
     */
    public void parse(File f) throws Exception {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.newSAXParser().parse(f, handler);
        } catch (ParserConfigurationException e) {
            throw new Exception("The underlying parser does not support "
                                + " the requested features");
        } catch (SAXException e) {
            throw new Exception("Error parsing XML document: " + e);
        }
    }

    /**
     * Return model name
     * @return model name
     */
    public String getModelName() {
        return handler.modelName;
    }
    
    /**
     * Return list of class descriptors
     * @return list of class descriptors
     */
    public List getClasses() {
        return handler.classes;
    } 

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class ModelHandler extends DefaultHandler
    {
        String modelName;
        List classes = new ArrayList();
        SkeletonClass cls;
   
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.equals("model")) {
                modelName = attrs.getValue("name");
            } else if (qName.equals("class")) {
                String name = attrs.getValue("name");
                String extend = attrs.getValue("extends");
                String implement = attrs.getValue("implements");
                boolean isInterface = new Boolean(attrs.getValue("is-interface"))
                    .booleanValue();
                cls = new SkeletonClass(name, extend, implement, isInterface);
            } else if (qName.equals("attribute")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("type");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                cls.attributes.add(new AttributeDescriptor(name, primaryKey, type));
            } else if (qName.equals("reference")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                String reverseReference = attrs.getValue("reverse-reference");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                cls.references.add(new ReferenceDescriptor(name, primaryKey, type,
                                                           reverseReference));
            } else if (qName.equals("collection")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                boolean ordered = new Boolean(attrs.getValue("ordered")).booleanValue();
                String reverseReference = attrs.getValue("reverse-reference");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                cls.collections.add(new CollectionDescriptor(name, primaryKey, type,
                                                             reverseReference, ordered));
            }
        }
    
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("class")) {
                classes.add(new ClassDescriptor(cls.name, cls.extend, cls.implement,
                                                cls.isInterface, cls.attributes, cls.references,
                                                cls.collections));
            }
        }
    }
    
    /**
     * Semi-constructed ClassDescriptor
     */
    class SkeletonClass
    {
        String name, extend, implement;
        boolean isInterface;
        List attributes = new ArrayList();
        List references = new ArrayList();
        List collections = new ArrayList();
        /**
         * Constructor
         * @param name the fully qualified name of the described class
         * @param extend the fully qualified super class name if one exists
         * @param implement a space string of fully qualified interface names
         * @param isInterface true if describing an interface
         */
        SkeletonClass(String name, String extend, String implement, boolean isInterface) {
            this.name = name;
            this.extend = extend;
            this.implement = implement;
            this.isInterface = isInterface;
        }
    }
}
