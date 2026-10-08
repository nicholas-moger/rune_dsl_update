package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationPathResolutionTest {

    private final BuiltinTypeRegistry builtins = BuiltinTypeRegistry.createDefault();
    private final TypeDirectedResolver resolver = new TypeDirectedResolver(builtins);

    // === Single segment resolution ===========================================

    @Test void single_segment_resolved() {
        // Trade { party: Party }
        // set result -> party: ...
        var tsResolver = new TestSymbolResolver();
        var party = new RDataType(); party.setName("Party");
        party.attachToWorkspace(tsResolver);
        tsResolver.bind("test", "Party", party);
        var partyTc = new RTypeCall(); partyTc.setTypeName("Party");
        partyTc.attachToWorkspace(tsResolver);
        partyTc.setReferencedTypeId(tsResolver.idFor("test", "Party"));
        var partyAttr = new RAttribute();
        partyAttr.setName("party"); partyAttr.setTypeCall(partyTc);
        var trade = new RDataType(); trade.setName("Trade");
        trade.attributes().add(partyAttr);

        var seg = new RSegment();
        seg.setName("party");

        RMetaAnnotatedType tradeType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveOperationPath(seg, tradeType);

        assertTrue(seg.resolvedAttribute().isPresent());
        assertEquals("party", seg.resolvedAttribute().get().name());
        Reference.reachabilityFence(tsResolver);
    }

    // === Chain resolution: foo -> bar -> baz =================================

    @Test void chain_resolved() {
        // Trade { party: Party { name: string } }
        // set result -> party -> name: ...
        var tsResolver = new TestSymbolResolver();
        var nameTc = new RTypeCall(); nameTc.setTypeName("string");
        // "string" is a builtin — no referencedTypeId set; TypeDirectedResolver falls back by name
        var nameAttr = new RAttribute();
        nameAttr.setName("name"); nameAttr.setTypeCall(nameTc);

        var party = new RDataType(); party.setName("Party");
        party.attributes().add(nameAttr);
        party.attachToWorkspace(tsResolver);
        tsResolver.bind("test", "Party", party);

        var partyTc = new RTypeCall(); partyTc.setTypeName("Party");
        partyTc.attachToWorkspace(tsResolver);
        partyTc.setReferencedTypeId(tsResolver.idFor("test", "Party"));
        var partyAttr = new RAttribute();
        partyAttr.setName("party"); partyAttr.setTypeCall(partyTc);

        var trade = new RDataType(); trade.setName("Trade");
        trade.attributes().add(partyAttr);

        // Build chain: party -> name
        var nameSeg = new RSegment(); nameSeg.setName("name");
        var partySeg = new RSegment(); partySeg.setName("party"); partySeg.setNext(nameSeg);

        RMetaAnnotatedType tradeType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveOperationPath(partySeg, tradeType);

        assertTrue(partySeg.resolvedAttribute().isPresent());
        assertEquals("party", partySeg.resolvedAttribute().get().name());
        assertTrue(nameSeg.resolvedAttribute().isPresent());
        assertEquals("name", nameSeg.resolvedAttribute().get().name());
        Reference.reachabilityFence(tsResolver);
    }

    // === Chain with unresolvable segment =====================================

    @Test void chain_stops_on_unresolvable() {
        var trade = new RDataType(); trade.setName("Trade");
        // No attributes

        var seg = new RSegment(); seg.setName("missing");
        RMetaAnnotatedType tradeType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveOperationPath(seg, tradeType);

        assertTrue(seg.resolvedAttribute().isEmpty());
    }

    // === Missing context type ================================================

    @Test void missing_context_no_resolution() {
        var seg = new RSegment(); seg.setName("foo");
        resolver.resolveOperationPath(seg, RMetaAnnotatedType.MISSING);
        assertTrue(seg.resolvedAttribute().isEmpty());
    }
}
