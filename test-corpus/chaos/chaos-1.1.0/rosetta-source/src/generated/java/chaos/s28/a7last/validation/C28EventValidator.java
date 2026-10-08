package chaos.s28.a7last.validation;

import chaos.s28.a7last.C28Event;
import chaos.s28.a7last.C28Extra;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C28EventValidator implements Validator<C28Event> {

	private List<ComparisonResult> getComparisonResults(C28Event o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (String) o.getKind() != null ? 1 : 0, 0, 1), 
				checkCardinality("size", (BigDecimal) o.getSize() != null ? 1 : 0, 0, 1), 
				checkCardinality("extra", (C28Extra) o.getExtra() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Event o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28Event", ValidationResult.ValidationType.CARDINALITY, "C28Event", path, "", res.getError());
				}
				return success("C28Event", ValidationResult.ValidationType.CARDINALITY, "C28Event", path, "");
			})
			.collect(toList());
	}

}
