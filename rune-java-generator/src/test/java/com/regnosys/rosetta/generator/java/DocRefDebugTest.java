package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

class DocRefDebugTest {

    private static final Path CDM_ROSETTA_DIR = Path.of("../common-domain-model/rosetta-source/src/main/rosetta");

    static boolean cdmAvailable() {
        return Files.isDirectory(CDM_ROSETTA_DIR);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void inspect_BusinessCenterEnum_doc_refs() {
        RModel model = AstBuilder.buildFromFile(CDM_ROSETTA_DIR.resolve("base-datetime-enum.rosetta"));
        for (var elem : model.rootElements()) {
            if (elem instanceof REnumeration en && en.name().equals("BusinessCenterEnum")) {
                System.out.println("ENUM: " + en.name());
                System.out.println("docRefs count: " + en.docReferences().size());
                for (var ref : en.docReferences()) {
                    System.out.println("  regulatoryRef: " + ref.isRegulatoryReference());
                    var dr = ref.regulatoryDocRef();
                    System.out.println("  regulatoryDocRef: " + (dr != null));
                    if (dr != null) {
                        System.out.println("    bodyRef: " + dr.bodyRef());
                        System.out.println("    corpusRefs: " + dr.corpusRefs());
                        System.out.println("    segmentRefs count: " + dr.segmentRefs().size());
                        for (var seg : dr.segmentRefs()) {
                            System.out.println("      seg: name=" + seg.segmentName() + " value=" + seg.value());
                        }
                    }
                    System.out.println("  provision: " + ref.provision());
                }
                return;
            }
        }
        System.out.println("BusinessCenterEnum not found");
    }
}
