package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.UtilRefs;
import holdout.typenamedutil.validation.UtilRefsTypeFormatValidator;
import holdout.typenamedutil.validation.UtilRefsValidator;
import holdout.typenamedutil.validation.exists.UtilRefsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=UtilRefs.class)
public class UtilRefsMeta implements RosettaMetaData<UtilRefs> {

	@Override
	public List<Validator<? super UtilRefs>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super UtilRefs, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super UtilRefs> validator(ValidatorFactory factory) {
		return factory.<UtilRefs>create(UtilRefsValidator.class);
	}

	@Override
	public Validator<? super UtilRefs> typeFormatValidator(ValidatorFactory factory) {
		return factory.<UtilRefs>create(UtilRefsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super UtilRefs> validator() {
		return new UtilRefsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super UtilRefs> typeFormatValidator() {
		return new UtilRefsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super UtilRefs, Set<String>> onlyExistsValidator() {
		return new UtilRefsOnlyExistsValidator();
	}
}
