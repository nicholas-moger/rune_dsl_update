package test.reg.meta;

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
import test.reg.Organisation;
import test.reg.validation.OrganisationTypeFormatValidator;
import test.reg.validation.OrganisationValidator;
import test.reg.validation.exists.OrganisationOnlyExistsValidator;


/**
 * @version test
 */
@RosettaMeta(model=Organisation.class)
public class OrganisationMeta implements RosettaMetaData<Organisation> {

	@Override
	public List<Validator<? super Organisation>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Organisation, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Organisation> validator(ValidatorFactory factory) {
		return factory.<Organisation>create(OrganisationValidator.class);
	}

	@Override
	public Validator<? super Organisation> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Organisation>create(OrganisationTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Organisation> validator() {
		return new OrganisationValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Organisation> typeFormatValidator() {
		return new OrganisationTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Organisation, Set<String>> onlyExistsValidator() {
		return new OrganisationOnlyExistsValidator();
	}
}
