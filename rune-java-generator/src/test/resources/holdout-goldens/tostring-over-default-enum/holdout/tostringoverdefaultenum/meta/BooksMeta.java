package holdout.tostringoverdefaultenum.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.tostringoverdefaultenum.Books;
import holdout.tostringoverdefaultenum.validation.BooksTypeFormatValidator;
import holdout.tostringoverdefaultenum.validation.BooksValidator;
import holdout.tostringoverdefaultenum.validation.exists.BooksOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Books.class)
public class BooksMeta implements RosettaMetaData<Books> {

	@Override
	public List<Validator<? super Books>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Books, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Books> validator(ValidatorFactory factory) {
		return factory.<Books>create(BooksValidator.class);
	}

	@Override
	public Validator<? super Books> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Books>create(BooksTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Books> validator() {
		return new BooksValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Books> typeFormatValidator() {
		return new BooksTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Books, Set<String>> onlyExistsValidator() {
		return new BooksOnlyExistsValidator();
	}
}
