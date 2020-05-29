package edu.gatech.gtri.trustmark.v1_0.impl.io.xml.producers;

import edu.gatech.gtri.trustmark.v1_0.io.xml.XmlProducer;
import edu.gatech.gtri.trustmark.v1_0.model.Source;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static edu.gatech.gtri.trustmark.v1_0.impl.TrustmarkFrameworkConstants.NAMESPACE_URI;

/**
 * Created by brad on 1/7/16.
 */
public class SourceXmlProducer extends AbstractXmlProducer implements XmlProducer {

    private static final Logger log = Logger.getLogger(SourceXmlProducer.class);

    @Override
    public Class getSupportedType() {
        return Source.class;
    }

    @Override
    public void serialize(Object instance, XMLStreamWriter xmlWriter) throws XMLStreamException {
        if( instance == null || !(instance instanceof Source) )
            throw new IllegalArgumentException("Invalid argument passed to "+this.getClass().getSimpleName()+"!  Expecting non-null instance of class["+this.getSupportedType().getName()+"]!");

        Source source = (Source) instance;

        log.debug("Writing XML for Source["+source.getIdentifier()+"]...");

        String id = "Source"+source.getIdentifier().hashCode(); // Should be unique for all sources, and a source should always resolve this, because we write refs to this later.
        log.debug("  generated source id: "+id);
        xmlWriter.writeAttribute("tf", NAMESPACE_URI, "id", id);

        log.debug("Writing identifier["+source.getIdentifier()+"]...");
        xmlWriter.writeStartElement(NAMESPACE_URI, "Identifier");
        xmlWriter.writeCharacters(source.getIdentifier());
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement(NAMESPACE_URI, "Reference");
        xmlWriter.writeCharacters(source.getReference());
        xmlWriter.writeEndElement();

        log.debug("Successfully wrote Source XML!");
    }//end serialize()


}//end TrustmarkStatusReportXmlProducer