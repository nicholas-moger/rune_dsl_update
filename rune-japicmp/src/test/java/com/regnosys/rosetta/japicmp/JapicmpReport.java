package com.regnosys.rosetta.japicmp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Minimal StAX reader for japicmp XML reports. Extracts the counts that
 * Layer-1 gate callers need: total classes reported and classes whose
 * {@code binaryCompatible="false"} attribute is set.
 *
 * <p>XML schema reference: japicmp-maven-plugin 0.23.1 emits classes under
 * {@code /japicmp/classes/class} with a {@code binaryCompatible} attribute
 * on each. Method and field nodes have their own {@code binaryCompatible}
 * attributes but a non-compat method implies a non-compat class, so class-
 * level counting is sufficient for the merge-blocker gate. Widen to method-
 * level by extending this class when future ignore-list workflows demand it;
 * do not reach into the XML via regex.
 *
 * <p>The japicmp XML is structured content, so StAX pull-parsing is the
 * correct shape here — not regex. (Same principle applies to grammar-
 * level content such as `.rosetta`, generated Java, etc.; the project's
 * maintainer-local engineering notes call this out specifically.)
 */
public final class JapicmpReport {

    private final int totalClassCount;
    private final int binaryIncompatibleCount;

    private JapicmpReport(int totalClassCount, int binaryIncompatibleCount) {
        this.totalClassCount = totalClassCount;
        this.binaryIncompatibleCount = binaryIncompatibleCount;
    }

    public int totalClassCount() {
        return totalClassCount;
    }

    public int binaryIncompatibleCount() {
        return binaryIncompatibleCount;
    }

    public static JapicmpReport parse(Path xml) {
        if (!Files.exists(xml)) {
            throw new IllegalStateException(
                "japicmp report missing at " + xml + "; "
                    + "run `mvn -f rune-japicmp/pom.xml verify` first"
            );
        }
        XMLInputFactory factory;
        try {
            factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            // Disable DTD + external entities — the japicmp report has no DTD
            // and is emitted by the plugin itself, but the report is also
            // sometimes archived and consumed elsewhere; hardening here is
            // cheap defence against a future XXE-shaped surprise.
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        } catch (IllegalArgumentException e) {
            // Some StAX implementations do not recognize one of the
            // properties above and throw here. Surface that as a
            // single actionable error rather than letting it escape
            // unwrapped from the parse() contract.
            throw new IllegalStateException(
                "Failed to configure XML parser for " + xml
                    + " — StAX implementation does not recognise a required property", e
            );
        }
        try (InputStream in = Files.newInputStream(xml)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                int total = 0;
                int incompat = 0;
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT
                        && "class".equals(reader.getLocalName())) {
                        total++;
                        String flag = reader.getAttributeValue(null, "binaryCompatible");
                        if ("false".equals(flag)) {
                            incompat++;
                        }
                    }
                }
                return new JapicmpReport(total, incompat);
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            throw new IllegalStateException("Failed to parse " + xml, e);
        }
    }
}
