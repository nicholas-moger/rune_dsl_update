package chaos.s31.x26enum.validation;

import chaos.s31.x26enum.C31Books;
import chaos.s31.x26enum.C31Held;
import chaos.s31.x26enum.C31MoveEnum;
import chaos.s31.x26enum.C31SideEnum;
import chaos.s31.x26enum.C31TopEnum;
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

public class C31BooksValidator implements Validator<C31Books> {

	private List<ComparisonResult> getComparisonResults(C31Books o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("side", (C31SideEnum) o.getSide() != null ? 1 : 0, 0, 1), 
				checkCardinality("move", (C31MoveEnum) o.getMove() != null ? 1 : 0, 0, 1), 
				checkCardinality("coded", (FieldWithMetaString) o.getCoded() != null ? 1 : 0, 0, 1), 
				checkCardinality("top", (C31TopEnum) o.getTop() != null ? 1 : 0, 0, 1), 
				checkCardinality("held", (C31Held) o.getHeld() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C31Books o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C31Books", ValidationResult.ValidationType.CARDINALITY, "C31Books", path, "", res.getError());
				}
				return success("C31Books", ValidationResult.ValidationType.CARDINALITY, "C31Books", path, "");
			})
			.collect(toList());
	}

}
