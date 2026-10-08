package test.aliasfilescope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasfilescope.WrittenSingle;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class WrittenSingleValidator implements Validator<WrittenSingle> {

	private List<ComparisonResult> getComparisonResults(WrittenSingle o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("Consumer", (Integer) o.getConsumer() != null ? 1 : 0, 0, 1), 
				checkCardinality("Object", (Integer) o.getObject() != null ? 1 : 0, 0, 1), 
				checkCardinality("Override", (Integer) o.getOverride() != null ? 1 : 0, 0, 1), 
				checkCardinality("Integer", (Integer) o.getInteger() != null ? 1 : 0, 0, 1), 
				checkCardinality("Objects", (Integer) o.getObjects() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, WrittenSingle o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("WrittenSingle", ValidationResult.ValidationType.CARDINALITY, "WrittenSingle", path, "", res.getError());
				}
				return success("WrittenSingle", ValidationResult.ValidationType.CARDINALITY, "WrittenSingle", path, "");
			})
			.collect(toList());
	}

}
