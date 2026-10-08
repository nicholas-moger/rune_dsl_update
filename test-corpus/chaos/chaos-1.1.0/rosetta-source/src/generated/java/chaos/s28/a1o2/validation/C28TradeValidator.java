package chaos.s28.a1o2.validation;

import chaos.s28.a1o2.C28Extra;
import chaos.s28.a1o2.C28Trade;
import chaos.s28.a1o2.C28Which;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C28TradeValidator implements Validator<C28Trade> {

	private List<ComparisonResult> getComparisonResults(C28Trade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utid", (String) o.getUtid() != null ? 1 : 0, 1, 1), 
				checkCardinality("which", (C28Which) o.getWhich() != null ? 1 : 0, 0, 1), 
				checkCardinality("venue", (FieldWithMetaString) o.getVenue() != null ? 1 : 0, 0, 1), 
				checkCardinality("extra", (C28Extra) o.getExtra() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28Trade", ValidationResult.ValidationType.CARDINALITY, "C28Trade", path, "", res.getError());
				}
				return success("C28Trade", ValidationResult.ValidationType.CARDINALITY, "C28Trade", path, "");
			})
			.collect(toList());
	}

}
