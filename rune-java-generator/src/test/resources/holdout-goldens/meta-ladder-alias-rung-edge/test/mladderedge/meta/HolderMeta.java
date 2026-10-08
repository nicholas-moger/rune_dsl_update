package test.mladderedge.meta;

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
import test.mladderedge.Holder;
import test.mladderedge.validation.HolderTypeFormatValidator;
import test.mladderedge.validation.HolderValidator;
import test.mladderedge.validation.exists.HolderOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Holder.class)
public class HolderMeta implements RosettaMetaData<Holder> {

	@Override
	public List<Validator<? super Holder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Holder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Holder> validator(ValidatorFactory factory) {
		return factory.<Holder>create(HolderValidator.class);
	}

	@Override
	public Validator<? super Holder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Holder>create(HolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Holder> validator() {
		return new HolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Holder> typeFormatValidator() {
		return new HolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Holder, Set<String>> onlyExistsValidator() {
		return new HolderOnlyExistsValidator();
	}
}
