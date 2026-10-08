package chaos.s20.a4snap.meta;

import chaos.s20.a4snap.C20Trunk;
import chaos.s20.a4snap.validation.C20TrunkTypeFormatValidator;
import chaos.s20.a4snap.validation.C20TrunkValidator;
import chaos.s20.a4snap.validation.exists.C20TrunkOnlyExistsValidator;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaMeta(model=C20Trunk.class)
public class C20TrunkMeta implements RosettaMetaData<C20Trunk> {

	@Override
	public List<Validator<? super C20Trunk>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C20Trunk, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C20Trunk> validator(ValidatorFactory factory) {
		return factory.<C20Trunk>create(C20TrunkValidator.class);
	}

	@Override
	public Validator<? super C20Trunk> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C20Trunk>create(C20TrunkTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C20Trunk> validator() {
		return new C20TrunkValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C20Trunk> typeFormatValidator() {
		return new C20TrunkTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C20Trunk, Set<String>> onlyExistsValidator() {
		return new C20TrunkOnlyExistsValidator();
	}
}
