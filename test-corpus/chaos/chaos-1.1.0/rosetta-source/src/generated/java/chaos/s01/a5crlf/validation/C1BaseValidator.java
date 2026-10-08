package chaos.s01.a5crlf.validation;

import chaos.s01.a5crlf.C1Base;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C1BaseValidator implements Validator<C1Base> {

	private List<ComparisonResult> getComparisonResults(C1Base o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("baseId", (String) o.getBaseId() != null ? 1 : 0, 1, 1), 
				checkCardinality("note", (String) o.getNote() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Base o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Base", ValidationResult.ValidationType.CARDINALITY, "C1Base", path, "", res.getError());
				}
				return success("C1Base", ValidationResult.ValidationType.CARDINALITY, "C1Base", path, "");
			})
			.collect(toList());
	}

}
