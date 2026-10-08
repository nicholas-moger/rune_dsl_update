package chaos.s20.a3half.p1.meta;

import chaos.s20.a3half.p1.C20Branch;
import chaos.s20.a3half.p1.validation.C20BranchTypeFormatValidator;
import chaos.s20.a3half.p1.validation.C20BranchValidator;
import chaos.s20.a3half.p1.validation.exists.C20BranchOnlyExistsValidator;
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
@RosettaMeta(model=C20Branch.class)
public class C20BranchMeta implements RosettaMetaData<C20Branch> {

	@Override
	public List<Validator<? super C20Branch>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C20Branch, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C20Branch> validator(ValidatorFactory factory) {
		return factory.<C20Branch>create(C20BranchValidator.class);
	}

	@Override
	public Validator<? super C20Branch> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C20Branch>create(C20BranchTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C20Branch> validator() {
		return new C20BranchValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C20Branch> typeFormatValidator() {
		return new C20BranchTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C20Branch, Set<String>> onlyExistsValidator() {
		return new C20BranchOnlyExistsValidator();
	}
}
