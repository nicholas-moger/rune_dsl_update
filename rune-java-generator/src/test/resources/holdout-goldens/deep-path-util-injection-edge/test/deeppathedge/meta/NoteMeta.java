package test.deeppathedge.meta;

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
import test.deeppathedge.Note;
import test.deeppathedge.validation.NoteTypeFormatValidator;
import test.deeppathedge.validation.NoteValidator;
import test.deeppathedge.validation.exists.NoteOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Note.class)
public class NoteMeta implements RosettaMetaData<Note> {

	@Override
	public List<Validator<? super Note>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Note, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Note> validator(ValidatorFactory factory) {
		return factory.<Note>create(NoteValidator.class);
	}

	@Override
	public Validator<? super Note> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Note>create(NoteTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Note> validator() {
		return new NoteValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Note> typeFormatValidator() {
		return new NoteTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Note, Set<String>> onlyExistsValidator() {
		return new NoteOnlyExistsValidator();
	}
}
