package chaos.s21.a2dangle.unused.meta;

import chaos.s21.a2dangle.unused.C21AuxUnusedT;
import chaos.s21.a2dangle.unused.validation.C21AuxUnusedTTypeFormatValidator;
import chaos.s21.a2dangle.unused.validation.C21AuxUnusedTValidator;
import chaos.s21.a2dangle.unused.validation.exists.C21AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C21AuxUnusedT.class)
public class C21AuxUnusedTMeta implements RosettaMetaData<C21AuxUnusedT> {

	@Override
	public List<Validator<? super C21AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C21AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C21AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C21AuxUnusedT>create(C21AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C21AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C21AuxUnusedT>create(C21AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C21AuxUnusedT> validator() {
		return new C21AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C21AuxUnusedT> typeFormatValidator() {
		return new C21AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C21AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C21AuxUnusedTOnlyExistsValidator();
	}
}
