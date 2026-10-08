package chaos.s17.a1o4.meta;

import chaos.s17.a1o4.C17Books;
import chaos.s17.a1o4.validation.C17BooksTypeFormatValidator;
import chaos.s17.a1o4.validation.C17BooksValidator;
import chaos.s17.a1o4.validation.datarule.C17BooksC17Agree;
import chaos.s17.a1o4.validation.exists.C17BooksOnlyExistsValidator;
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
@RosettaMeta(model=C17Books.class)
public class C17BooksMeta implements RosettaMetaData<C17Books> {

	@Override
	public List<Validator<? super C17Books>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C17Books>create(C17BooksC17Agree.class)
		);
	}
	
	@Override
	public List<Function<? super C17Books, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C17Books> validator(ValidatorFactory factory) {
		return factory.<C17Books>create(C17BooksValidator.class);
	}

	@Override
	public Validator<? super C17Books> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C17Books>create(C17BooksTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C17Books> validator() {
		return new C17BooksValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C17Books> typeFormatValidator() {
		return new C17BooksTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C17Books, Set<String>> onlyExistsValidator() {
		return new C17BooksOnlyExistsValidator();
	}
}
