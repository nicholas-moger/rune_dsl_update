package chaos.s09.a3half.p2.meta;

import chaos.s09.a3half.p2.C9Loc;
import chaos.s09.a3half.p2.validation.C9LocTypeFormatValidator;
import chaos.s09.a3half.p2.validation.C9LocValidator;
import chaos.s09.a3half.p2.validation.exists.C9LocOnlyExistsValidator;
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
@RosettaMeta(model=C9Loc.class)
public class C9LocMeta implements RosettaMetaData<C9Loc> {

	@Override
	public List<Validator<? super C9Loc>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C9Loc, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C9Loc> validator(ValidatorFactory factory) {
		return factory.<C9Loc>create(C9LocValidator.class);
	}

	@Override
	public Validator<? super C9Loc> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C9Loc>create(C9LocTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C9Loc> validator() {
		return new C9LocValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C9Loc> typeFormatValidator() {
		return new C9LocTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C9Loc, Set<String>> onlyExistsValidator() {
		return new C9LocOnlyExistsValidator();
	}
}
