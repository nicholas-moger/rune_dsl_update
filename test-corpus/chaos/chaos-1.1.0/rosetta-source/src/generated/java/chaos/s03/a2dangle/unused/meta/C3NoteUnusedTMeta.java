package chaos.s03.a2dangle.unused.meta;

import chaos.s03.a2dangle.unused.C3NoteUnusedT;
import chaos.s03.a2dangle.unused.validation.C3NoteUnusedTTypeFormatValidator;
import chaos.s03.a2dangle.unused.validation.C3NoteUnusedTValidator;
import chaos.s03.a2dangle.unused.validation.exists.C3NoteUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C3NoteUnusedT.class)
public class C3NoteUnusedTMeta implements RosettaMetaData<C3NoteUnusedT> {

	@Override
	public List<Validator<? super C3NoteUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C3NoteUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3NoteUnusedT> validator(ValidatorFactory factory) {
		return factory.<C3NoteUnusedT>create(C3NoteUnusedTValidator.class);
	}

	@Override
	public Validator<? super C3NoteUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3NoteUnusedT>create(C3NoteUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3NoteUnusedT> validator() {
		return new C3NoteUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3NoteUnusedT> typeFormatValidator() {
		return new C3NoteUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3NoteUnusedT, Set<String>> onlyExistsValidator() {
		return new C3NoteUnusedTOnlyExistsValidator();
	}
}
