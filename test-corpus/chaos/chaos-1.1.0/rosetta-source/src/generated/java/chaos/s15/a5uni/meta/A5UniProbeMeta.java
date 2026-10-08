package chaos.s15.a5uni.meta;

import chaos.s15.a5uni.A5UniProbe;
import chaos.s15.a5uni.validation.A5UniProbeTypeFormatValidator;
import chaos.s15.a5uni.validation.A5UniProbeValidator;
import chaos.s15.a5uni.validation.exists.A5UniProbeOnlyExistsValidator;
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
@RosettaMeta(model=A5UniProbe.class)
public class A5UniProbeMeta implements RosettaMetaData<A5UniProbe> {

	@Override
	public List<Validator<? super A5UniProbe>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super A5UniProbe, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super A5UniProbe> validator(ValidatorFactory factory) {
		return factory.<A5UniProbe>create(A5UniProbeValidator.class);
	}

	@Override
	public Validator<? super A5UniProbe> typeFormatValidator(ValidatorFactory factory) {
		return factory.<A5UniProbe>create(A5UniProbeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super A5UniProbe> validator() {
		return new A5UniProbeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super A5UniProbe> typeFormatValidator() {
		return new A5UniProbeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super A5UniProbe, Set<String>> onlyExistsValidator() {
		return new A5UniProbeOnlyExistsValidator();
	}
}
