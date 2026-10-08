package test.deeppathedgetypes.meta;

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
import test.deeppathedgetypes.RNote;
import test.deeppathedgetypes.validation.RNoteTypeFormatValidator;
import test.deeppathedgetypes.validation.RNoteValidator;
import test.deeppathedgetypes.validation.exists.RNoteOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RNote.class)
public class RNoteMeta implements RosettaMetaData<RNote> {

	@Override
	public List<Validator<? super RNote>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RNote, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RNote> validator(ValidatorFactory factory) {
		return factory.<RNote>create(RNoteValidator.class);
	}

	@Override
	public Validator<? super RNote> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RNote>create(RNoteTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RNote> validator() {
		return new RNoteValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RNote> typeFormatValidator() {
		return new RNoteTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RNote, Set<String>> onlyExistsValidator() {
		return new RNoteOnlyExistsValidator();
	}
}
