package chaos.s01.a1o1.validation;

import chaos.s01.a1o1.C1Leaf;
import chaos.s01.a1o1.C1Ref;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C1LeafValidator implements Validator<C1Leaf> {

	private List<ComparisonResult> getComparisonResults(C1Leaf o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("baseId", (String) o.getBaseId() != null ? 1 : 0, 1, 1), 
				checkCardinality("note", (String) o.getNote() != null ? 1 : 0, 1, 1), 
				checkCardinality("asOf", (Date) o.getAsOf() != null ? 1 : 0, 0, 1), 
				checkCardinality("parts", (List<? extends C1Ref>) o.getParts() == null ? 0 : o.getParts().size(), 2, 0), 
				checkCardinality("ratio", (BigDecimal) o.getRatio() != null ? 1 : 0, 0, 1), 
				checkCardinality("code", (String) o.getCode() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Leaf o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Leaf", ValidationResult.ValidationType.CARDINALITY, "C1Leaf", path, "", res.getError());
				}
				return success("C1Leaf", ValidationResult.ValidationType.CARDINALITY, "C1Leaf", path, "");
			})
			.collect(toList());
	}

}
