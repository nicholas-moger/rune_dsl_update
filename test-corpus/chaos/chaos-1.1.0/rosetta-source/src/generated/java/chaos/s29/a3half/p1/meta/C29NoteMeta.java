package chaos.s29.a3half.p1.meta;

import chaos.s29.a3half.p1.C29Note;
import chaos.s29.a3half.p1.validation.C29NoteTypeFormatValidator;
import chaos.s29.a3half.p1.validation.C29NoteValidator;
import chaos.s29.a3half.p1.validation.exists.C29NoteOnlyExistsValidator;
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
@RosettaMeta(model=C29Note.class)
public class C29NoteMeta implements RosettaMetaData<C29Note> {

	@Override
	public List<Validator<? super C29Note>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29Note, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Note> validator(ValidatorFactory factory) {
		return factory.<C29Note>create(C29NoteValidator.class);
	}

	@Override
	public Validator<? super C29Note> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Note>create(C29NoteTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Note> validator() {
		return new C29NoteValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Note> typeFormatValidator() {
		return new C29NoteTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Note, Set<String>> onlyExistsValidator() {
		return new C29NoteOnlyExistsValidator();
	}
}
