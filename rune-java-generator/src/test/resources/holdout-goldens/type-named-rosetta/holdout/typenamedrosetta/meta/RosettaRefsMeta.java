package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.RosettaRefs;
import holdout.typenamedrosetta.validation.RosettaRefsTypeFormatValidator;
import holdout.typenamedrosetta.validation.RosettaRefsValidator;
import holdout.typenamedrosetta.validation.exists.RosettaRefsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaRefs.class)
public class RosettaRefsMeta implements RosettaMetaData<RosettaRefs> {

	@Override
	public List<Validator<? super RosettaRefs>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaRefs, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaRefs> validator(ValidatorFactory factory) {
		return factory.<RosettaRefs>create(RosettaRefsValidator.class);
	}

	@Override
	public Validator<? super RosettaRefs> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaRefs>create(RosettaRefsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaRefs> validator() {
		return new RosettaRefsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaRefs> typeFormatValidator() {
		return new RosettaRefsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaRefs, Set<String>> onlyExistsValidator() {
		return new RosettaRefsOnlyExistsValidator();
	}
}
