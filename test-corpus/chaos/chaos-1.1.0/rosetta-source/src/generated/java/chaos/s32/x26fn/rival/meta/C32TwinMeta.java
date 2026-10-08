package chaos.s32.x26fn.rival.meta;

import chaos.s32.x26fn.rival.C32Twin;
import chaos.s32.x26fn.rival.validation.C32TwinTypeFormatValidator;
import chaos.s32.x26fn.rival.validation.C32TwinValidator;
import chaos.s32.x26fn.rival.validation.exists.C32TwinOnlyExistsValidator;
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
@RosettaMeta(model=C32Twin.class)
public class C32TwinMeta implements RosettaMetaData<C32Twin> {

	@Override
	public List<Validator<? super C32Twin>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C32Twin, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C32Twin> validator(ValidatorFactory factory) {
		return factory.<C32Twin>create(C32TwinValidator.class);
	}

	@Override
	public Validator<? super C32Twin> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C32Twin>create(C32TwinTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C32Twin> validator() {
		return new C32TwinValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C32Twin> typeFormatValidator() {
		return new C32TwinTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C32Twin, Set<String>> onlyExistsValidator() {
		return new C32TwinOnlyExistsValidator();
	}
}
