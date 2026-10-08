package holdout.tostringoverdefaultenum.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.tostringoverdefaultenum.Books;
import holdout.tostringoverdefaultenum.SideEnum;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BooksValidator implements Validator<Books> {

	private List<ComparisonResult> getComparisonResults(Books o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("side", (SideEnum) o.getSide() != null ? 1 : 0, 0, 1), 
				checkCardinality("coded", (FieldWithMetaString) o.getCoded() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Books o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Books", ValidationResult.ValidationType.CARDINALITY, "Books", path, "", res.getError());
				}
				return success("Books", ValidationResult.ValidationType.CARDINALITY, "Books", path, "");
			})
			.collect(toList());
	}

}
