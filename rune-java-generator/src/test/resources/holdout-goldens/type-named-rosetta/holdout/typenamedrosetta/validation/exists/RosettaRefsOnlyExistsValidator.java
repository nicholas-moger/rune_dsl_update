package holdout.typenamedrosetta.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.BuilderMerger;
import holdout.typenamedrosetta.BuilderProcessor;
import holdout.typenamedrosetta.ComparisonResult;
import holdout.typenamedrosetta.ListEquals;
import holdout.typenamedrosetta.Processor;
import holdout.typenamedrosetta.QualifyFunctionFactory;
import holdout.typenamedrosetta.QualifyResult;
import holdout.typenamedrosetta.RosettaMetaData;
import holdout.typenamedrosetta.RosettaModelObject;
import holdout.typenamedrosetta.RosettaModelObjectBuilder;
import holdout.typenamedrosetta.RosettaRefs;
import holdout.typenamedrosetta.Validator;
import holdout.typenamedrosetta.ValidatorFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RosettaRefsOnlyExistsValidator implements ValidatorWithArg<RosettaRefs, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RosettaRefs> ValidationResult<RosettaRefs> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("validator", ExistenceChecker.isSet((Validator) o.getValidator()))
				.put("validationResult", ExistenceChecker.isSet((holdout.typenamedrosetta.ValidationResult) o.getValidationResult()))
				.put("validatorWithArg", ExistenceChecker.isSet((holdout.typenamedrosetta.ValidatorWithArg) o.getValidatorWithArg()))
				.put("rpath", ExistenceChecker.isSet((holdout.typenamedrosetta.RosettaPath) o.getRpath()))
				.put("comparisonResult", ExistenceChecker.isSet((ComparisonResult) o.getComparisonResult()))
				.put("existenceChecker", ExistenceChecker.isSet((holdout.typenamedrosetta.ExistenceChecker) o.getExistenceChecker()))
				.put("rosettaMetaData", ExistenceChecker.isSet((RosettaMetaData) o.getRosettaMetaData()))
				.put("validatorFactory", ExistenceChecker.isSet((ValidatorFactory) o.getValidatorFactory()))
				.put("qualifyResult", ExistenceChecker.isSet((QualifyResult) o.getQualifyResult()))
				.put("qualifyFunctionFactory", ExistenceChecker.isSet((QualifyFunctionFactory) o.getQualifyFunctionFactory()))
				.put("processor", ExistenceChecker.isSet((Processor) o.getProcessor()))
				.put("builderProcessor", ExistenceChecker.isSet((BuilderProcessor) o.getBuilderProcessor()))
				.put("builderMerger", ExistenceChecker.isSet((BuilderMerger) o.getBuilderMerger()))
				.put("rosettaModelObject", ExistenceChecker.isSet((RosettaModelObject) o.getRosettaModelObject()))
				.put("rosettaModelObjectBuilder", ExistenceChecker.isSet((RosettaModelObjectBuilder) o.getRosettaModelObjectBuilder()))
				.put("listEquals", ExistenceChecker.isSet((ListEquals) o.getListEquals()))
				.put("names", ExistenceChecker.isSet((List<String>) o.getNames()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RosettaRefs", ValidationResult.ValidationType.ONLY_EXISTS, "RosettaRefs", path, "");
		}
		return failure("RosettaRefs", ValidationResult.ValidationType.ONLY_EXISTS, "RosettaRefs", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
