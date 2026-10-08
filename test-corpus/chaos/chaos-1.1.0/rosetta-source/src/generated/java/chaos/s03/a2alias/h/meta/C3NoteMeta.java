package chaos.s03.a2alias.h.meta;

import chaos.s03.a2alias.h.C3Note;
import chaos.s03.a2alias.h.validation.C3NoteTypeFormatValidator;
import chaos.s03.a2alias.h.validation.C3NoteValidator;
import chaos.s03.a2alias.h.validation.exists.C3NoteOnlyExistsValidator;
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
@RosettaMeta(model=C3Note.class)
public class C3NoteMeta implements RosettaMetaData<C3Note> {

	@Override
	public List<Validator<? super C3Note>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C3Note, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3Note> validator(ValidatorFactory factory) {
		return factory.<C3Note>create(C3NoteValidator.class);
	}

	@Override
	public Validator<? super C3Note> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3Note>create(C3NoteTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3Note> validator() {
		return new C3NoteValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3Note> typeFormatValidator() {
		return new C3NoteTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3Note, Set<String>> onlyExistsValidator() {
		return new C3NoteOnlyExistsValidator();
	}
}
