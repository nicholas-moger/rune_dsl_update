package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RTypeFormatterTest {

    @Test
    void missingRendersAngleBracketToken() {
        assertEquals("<missing>", RTypeFormatter.format(RMetaAnnotatedType.MISSING));
    }

    @Test
    void basicTypeRendersLowercaseName() {
        assertEquals("boolean",
                RTypeFormatter.format(RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN)));
    }

    @Test
    void unconstrainedNumberRendersNameOnly() {
        assertEquals("number",
                RTypeFormatter.format(RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained())));
    }

    @Test
    void numberWithFractionalDigitsAppendsParam() {
        RNumberType n = new RNumberType(OptionalInt.empty(), OptionalInt.of(2),
                java.util.Optional.empty(), java.util.Optional.empty());
        assertEquals("number<fractionalDigits=2>",
                RTypeFormatter.format(RMetaAnnotatedType.withNoMeta(n)));
    }

    @Test
    void intTypeRendersIntWithRedundantButHonestFractionalDigits() {
        assertEquals("int<fractionalDigits=0>",
                RTypeFormatter.format(RMetaAnnotatedType.withNoMeta(RNumberType.intType())));
    }

    @Test
    void stringTypeRendersName() {
        assertEquals("string",
                RTypeFormatter.format(RMetaAnnotatedType.withNoMeta(RStringType.unconstrained())));
    }

    @Test
    void metaAttributesAreSortedAndAppended() {
        RMetaAnnotatedType t = RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN,
                List.of("scheme", "location"));
        assertEquals("boolean meta[location, scheme]", RTypeFormatter.format(t));
    }
}
