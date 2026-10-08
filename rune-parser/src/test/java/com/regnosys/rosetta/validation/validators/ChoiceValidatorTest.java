package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChoiceValidatorTest {

    private final ChoiceValidator validator = new ChoiceValidator();

    @Test void unique_options_pass() {
        var choice = makeChoice("Instrument", "Bond", "Equity");
        var c = new ValidationCollector();
        validator.validate(choice, c);
        assertTrue(c.isEmpty());
    }

    @Test void duplicate_options_error() {
        var choice = makeChoice("Instrument", "Bond", "Bond");
        var c = new ValidationCollector();
        validator.validate(choice, c);
        assertFalse(c.isEmpty());
        assertEquals(ValidationIssueCode.DUPLICATE_CHOICE_RULE_ATTRIBUTE, c.toList().get(0).issueCode());
    }

    @Test void single_option_passes() {
        var choice = makeChoice("Wrapper", "Inner");
        var c = new ValidationCollector();
        validator.validate(choice, c);
        assertTrue(c.isEmpty());
    }

    @Test void empty_options_passes() {
        var choice = new RChoice();
        choice.setName("Empty");
        var c = new ValidationCollector();
        validator.validate(choice, c);
        assertTrue(c.isEmpty());
    }

    @Test void non_choice_ignored() {
        var dt = new RDataType(); dt.setName("Foo");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    private RChoice makeChoice(String name, String... optionTypeNames) {
        var choice = new RChoice();
        choice.setName(name);
        for (String typeName : optionTypeNames) {
            var opt = new RChoiceOption();
            var tc = new RTypeCall();
            tc.setTypeName(typeName);
            opt.setTypeCall(tc);
            choice.options().add(opt);
        }
        return choice;
    }
}
