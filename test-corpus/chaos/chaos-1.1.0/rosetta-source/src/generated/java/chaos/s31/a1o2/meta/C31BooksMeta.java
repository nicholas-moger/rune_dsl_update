package chaos.s31.a1o2.meta;

import chaos.s31.a1o2.C31Books;
import chaos.s31.a1o2.validation.C31BooksTypeFormatValidator;
import chaos.s31.a1o2.validation.C31BooksValidator;
import chaos.s31.a1o2.validation.exists.C31BooksOnlyExistsValidator;
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
@RosettaMeta(model=C31Books.class)
public class C31BooksMeta implements RosettaMetaData<C31Books> {

	@Override
	public List<Validator<? super C31Books>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C31Books, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C31Books> validator(ValidatorFactory factory) {
		return factory.<C31Books>create(C31BooksValidator.class);
	}

	@Override
	public Validator<? super C31Books> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C31Books>create(C31BooksTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C31Books> validator() {
		return new C31BooksValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C31Books> typeFormatValidator() {
		return new C31BooksTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C31Books, Set<String>> onlyExistsValidator() {
		return new C31BooksOnlyExistsValidator();
	}
}
