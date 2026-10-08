package chaos.s16.a6choice.meta;

import chaos.s16.a6choice.C16Slot;
import chaos.s16.a6choice.validation.C16SlotTypeFormatValidator;
import chaos.s16.a6choice.validation.C16SlotValidator;
import chaos.s16.a6choice.validation.exists.C16SlotOnlyExistsValidator;
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
 * @version 1.0.0
 */
@RosettaMeta(model=C16Slot.class)
public class C16SlotMeta implements RosettaMetaData<C16Slot> {

	@Override
	public List<Validator<? super C16Slot>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16Slot, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16Slot> validator(ValidatorFactory factory) {
		return factory.<C16Slot>create(C16SlotValidator.class);
	}

	@Override
	public Validator<? super C16Slot> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16Slot>create(C16SlotTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16Slot> validator() {
		return new C16SlotValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16Slot> typeFormatValidator() {
		return new C16SlotTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16Slot, Set<String>> onlyExistsValidator() {
		return new C16SlotOnlyExistsValidator();
	}
}
