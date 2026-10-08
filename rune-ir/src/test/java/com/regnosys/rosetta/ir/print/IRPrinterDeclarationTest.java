package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IRPrinterDeclarationTest {

    @Test
    void escapesSpecialCharactersInDisplayName() {
        // displayName chars: a " b \ c <newline> d
        IREnumValue v = new IREnumValueNode("V", Optional.of("a\"b\\c\nd"),
                Optional.empty(), IRMetadata.EMPTY);
        IREnumNode e = new IREnumNode("E", List.of(v), Optional.empty(), IRMetadata.EMPTY);
        assertEquals("ENUM E\n  VALUE V displayName \"a\\\"b\\\\c\\nd\"\n",
                new IRPrinter().print(e));
    }

    @Test
    void printsEnumWithDisplayNamePresentAndAbsent() {
        IREnumValue up = new IREnumValueNode("Up", Optional.empty(),
                Optional.empty(), IRMetadata.EMPTY);
        IREnumValue down = new IREnumValueNode("Down", Optional.of("DOWN"),
                Optional.empty(), IRMetadata.EMPTY);
        IREnumNode e = new IREnumNode("test.model.DirectionEnum", List.of(up, down),
                Optional.empty(), IRMetadata.EMPTY);

        String out = new IRPrinter().print(e);

        assertEquals("""
                ENUM test.model.DirectionEnum
                  VALUE Up
                  VALUE Down displayName "DOWN"
                """, out);
    }

    @Test
    void printsStructWithFieldsInSourceOrderAndTypeByName() {
        IRFieldNode bar = new IRFieldNode("bar", typeRef("string"),
                Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY);
        IRFieldNode baz = new IRFieldNode("baz", typeRef("number"),
                Cardinality.ZERO_TO_MANY,
                Optional.empty(), IRMetadata.EMPTY);
        IRTypeNode foo = new IRTypeNode("test.model.Foo",
                IRKind.STRUCT, List.of(bar, baz),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                STRUCT test.model.Foo [abstract=false]
                  FIELD bar : string [ONE_TO_ONE]
                  FIELD baz : number [ZERO_TO_MANY]
                """, new IRPrinter().print(foo));
    }

    @Test
    void printsChoiceOptionsAsOneToOneFields() {
        IRTypeNode choice = new IRTypeNode("test.model.PaymentMethod",
                IRKind.CHOICE,
                List.of(
                    new IRFieldNode("CashPayment", typeRef("CashPayment"),
                        Cardinality.ONE_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY),
                    new IRFieldNode("CardPayment", typeRef("CardPayment"),
                        Cardinality.ONE_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY)),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                CHOICE test.model.PaymentMethod [abstract=false]
                  FIELD CashPayment : CashPayment [ONE_TO_ONE]
                  FIELD CardPayment : CardPayment [ONE_TO_ONE]
                """, new IRPrinter().print(choice));
    }

    @Test
    void printsBaseTypeAsExtendsByName() {
        IRTypeNode base = new IRTypeNode("test.model.Base",
                IRKind.STRUCT,
                List.of(new IRFieldNode("id", typeRef("string"),
                        Cardinality.ONE_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY)),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
        IRTypeNode child = new IRTypeNode("test.model.Child",
                IRKind.STRUCT,
                List.of(new IRFieldNode("extra", typeRef("int"),
                        Cardinality.ZERO_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY)),
                Optional.of(typeRef("Base")), false,
                Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                STRUCT test.model.Base [abstract=false]
                  FIELD id : string [ONE_TO_ONE]
                STRUCT test.model.Child : Base [abstract=false]
                  FIELD extra : int [ZERO_TO_ONE]
                """, new IRPrinter().printAll(List.of(base, child)));
    }

    /** A lightweight declaration type-ref: name + best-effort kind, no fields. */
    private static IRType typeRef(String name) {
        return new IRTypeNode(name, IRKind.STRUCT,
                List.of(), Optional.empty(), false,
                Optional.empty(), IRMetadata.EMPTY);
    }
}
