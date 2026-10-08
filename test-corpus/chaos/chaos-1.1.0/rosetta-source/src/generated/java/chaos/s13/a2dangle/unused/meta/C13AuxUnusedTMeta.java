package chaos.s13.a2dangle.unused.meta;

import chaos.s13.a2dangle.unused.C13AuxUnusedT;
import chaos.s13.a2dangle.unused.validation.C13AuxUnusedTTypeFormatValidator;
import chaos.s13.a2dangle.unused.validation.C13AuxUnusedTValidator;
import chaos.s13.a2dangle.unused.validation.exists.C13AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C13AuxUnusedT.class)
public class C13AuxUnusedTMeta implements RosettaMetaData<C13AuxUnusedT> {

	@Override
	public List<Validator<? super C13AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C13AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C13AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C13AuxUnusedT>create(C13AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C13AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C13AuxUnusedT>create(C13AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C13AuxUnusedT> validator() {
		return new C13AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C13AuxUnusedT> typeFormatValidator() {
		return new C13AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C13AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C13AuxUnusedTOnlyExistsValidator();
	}
}
