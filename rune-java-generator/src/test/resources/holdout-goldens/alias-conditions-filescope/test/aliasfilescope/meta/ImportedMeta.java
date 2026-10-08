package test.aliasfilescope.meta;

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
import test.aliasfilescope.Imported;
import test.aliasfilescope.validation.ImportedTypeFormatValidator;
import test.aliasfilescope.validation.ImportedValidator;
import test.aliasfilescope.validation.exists.ImportedOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Imported.class)
public class ImportedMeta implements RosettaMetaData<Imported> {

	@Override
	public List<Validator<? super Imported>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Imported, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Imported> validator(ValidatorFactory factory) {
		return factory.<Imported>create(ImportedValidator.class);
	}

	@Override
	public Validator<? super Imported> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Imported>create(ImportedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Imported> validator() {
		return new ImportedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Imported> typeFormatValidator() {
		return new ImportedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Imported, Set<String>> onlyExistsValidator() {
		return new ImportedOnlyExistsValidator();
	}
}
