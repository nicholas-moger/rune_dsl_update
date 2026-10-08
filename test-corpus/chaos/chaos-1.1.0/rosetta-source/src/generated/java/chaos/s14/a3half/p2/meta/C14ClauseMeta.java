package chaos.s14.a3half.p2.meta;

import chaos.s14.a3half.p2.C14Clause;
import chaos.s14.a3half.p2.validation.C14ClauseTypeFormatValidator;
import chaos.s14.a3half.p2.validation.C14ClauseValidator;
import chaos.s14.a3half.p2.validation.exists.C14ClauseOnlyExistsValidator;
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
@RosettaMeta(model=C14Clause.class)
public class C14ClauseMeta implements RosettaMetaData<C14Clause> {

	@Override
	public List<Validator<? super C14Clause>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C14Clause, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C14Clause> validator(ValidatorFactory factory) {
		return factory.<C14Clause>create(C14ClauseValidator.class);
	}

	@Override
	public Validator<? super C14Clause> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C14Clause>create(C14ClauseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C14Clause> validator() {
		return new C14ClauseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C14Clause> typeFormatValidator() {
		return new C14ClauseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C14Clause, Set<String>> onlyExistsValidator() {
		return new C14ClauseOnlyExistsValidator();
	}
}
