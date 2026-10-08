package base.layer.meta;

import base.layer.Instruction;
import base.layer.validation.InstructionTypeFormatValidator;
import base.layer.validation.InstructionValidator;
import base.layer.validation.exists.InstructionOnlyExistsValidator;
import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Instruction.class)
public class InstructionMeta implements RosettaMetaData<Instruction> {

	@Override
	public List<Validator<? super Instruction>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Instruction, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Instruction> validator(ValidatorFactory factory) {
		return factory.<Instruction>create(InstructionValidator.class);
	}

	@Override
	public Validator<? super Instruction> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Instruction>create(InstructionTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Instruction> validator() {
		return new InstructionValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Instruction> typeFormatValidator() {
		return new InstructionTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Instruction, Set<String>> onlyExistsValidator() {
		return new InstructionOnlyExistsValidator();
	}
}
