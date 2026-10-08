package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.BuilderMerger;
import holdout.typenamedrosetta.BuilderProcessor;
import holdout.typenamedrosetta.ExistenceChecker;
import holdout.typenamedrosetta.ListEquals;
import holdout.typenamedrosetta.Processor;
import holdout.typenamedrosetta.QualifyFunctionFactory;
import holdout.typenamedrosetta.QualifyResult;
import holdout.typenamedrosetta.RosettaMetaData;
import holdout.typenamedrosetta.RosettaModelObject;
import holdout.typenamedrosetta.RosettaModelObjectBuilder;
import holdout.typenamedrosetta.RosettaPath;
import holdout.typenamedrosetta.RosettaRefs;
import holdout.typenamedrosetta.ValidationResult;
import holdout.typenamedrosetta.ValidatorFactory;
import holdout.typenamedrosetta.ValidatorWithArg;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RosettaRefsValidator implements Validator<RosettaRefs> {

	private List<ComparisonResult> getComparisonResults(RosettaRefs o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("validator", (holdout.typenamedrosetta.Validator) o.getValidator() != null ? 1 : 0, 0, 1), 
				checkCardinality("validationResult", (ValidationResult) o.getValidationResult() != null ? 1 : 0, 0, 1), 
				checkCardinality("validatorWithArg", (ValidatorWithArg) o.getValidatorWithArg() != null ? 1 : 0, 0, 1), 
				checkCardinality("rpath", (RosettaPath) o.getRpath() != null ? 1 : 0, 0, 1), 
				checkCardinality("comparisonResult", (holdout.typenamedrosetta.ComparisonResult) o.getComparisonResult() != null ? 1 : 0, 0, 1), 
				checkCardinality("existenceChecker", (ExistenceChecker) o.getExistenceChecker() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaMetaData", (RosettaMetaData) o.getRosettaMetaData() != null ? 1 : 0, 0, 1), 
				checkCardinality("validatorFactory", (ValidatorFactory) o.getValidatorFactory() != null ? 1 : 0, 0, 1), 
				checkCardinality("qualifyResult", (QualifyResult) o.getQualifyResult() != null ? 1 : 0, 0, 1), 
				checkCardinality("qualifyFunctionFactory", (QualifyFunctionFactory) o.getQualifyFunctionFactory() != null ? 1 : 0, 0, 1), 
				checkCardinality("processor", (Processor) o.getProcessor() != null ? 1 : 0, 0, 1), 
				checkCardinality("builderProcessor", (BuilderProcessor) o.getBuilderProcessor() != null ? 1 : 0, 0, 1), 
				checkCardinality("builderMerger", (BuilderMerger) o.getBuilderMerger() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaModelObject", (RosettaModelObject) o.getRosettaModelObject() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaModelObjectBuilder", (RosettaModelObjectBuilder) o.getRosettaModelObjectBuilder() != null ? 1 : 0, 0, 1), 
				checkCardinality("listEquals", (ListEquals) o.getListEquals() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<com.rosetta.model.lib.validation.ValidationResult<?>> getValidationResults(com.rosetta.model.lib.path.RosettaPath path, RosettaRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RosettaRefs", com.rosetta.model.lib.validation.ValidationResult.ValidationType.CARDINALITY, "RosettaRefs", path, "", res.getError());
				}
				return success("RosettaRefs", com.rosetta.model.lib.validation.ValidationResult.ValidationType.CARDINALITY, "RosettaRefs", path, "");
			})
			.collect(toList());
	}

}
