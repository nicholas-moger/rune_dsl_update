package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.ArrayList;
import holdout.typenamedutil.Arrays;
import holdout.typenamedutil.BigDecimal;
import holdout.typenamedutil.Collections;
import holdout.typenamedutil.Collectors;
import holdout.typenamedutil.Consumer;
import holdout.typenamedutil.Function;
import holdout.typenamedutil.Map;
import holdout.typenamedutil.Objects;
import holdout.typenamedutil.Pattern;
import holdout.typenamedutil.Set;
import holdout.typenamedutil.UtilRefs;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class UtilRefsValidator implements Validator<UtilRefs> {

	private List<ComparisonResult> getComparisonResults(UtilRefs o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("map", (Map) o.getMap() != null ? 1 : 0, 0, 1), 
				checkCardinality("theSet", (Set) o.getTheSet() != null ? 1 : 0, 0, 1), 
				checkCardinality("objects", (Objects) o.getObjects() != null ? 1 : 0, 0, 1), 
				checkCardinality("collectors", (Collectors) o.getCollectors() != null ? 1 : 0, 0, 1), 
				checkCardinality("arrays", (Arrays) o.getArrays() != null ? 1 : 0, 0, 1), 
				checkCardinality("collections", (Collections) o.getCollections() != null ? 1 : 0, 0, 1), 
				checkCardinality("fn", (Function) o.getFn() != null ? 1 : 0, 0, 1), 
				checkCardinality("consumer", (Consumer) o.getConsumer() != null ? 1 : 0, 0, 1), 
				checkCardinality("arrayList", (ArrayList) o.getArrayList() != null ? 1 : 0, 0, 1), 
				checkCardinality("pat", (Pattern) o.getPat() != null ? 1 : 0, 0, 1), 
				checkCardinality("bigDecimal", (BigDecimal) o.getBigDecimal() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, UtilRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("UtilRefs", ValidationResult.ValidationType.CARDINALITY, "UtilRefs", path, "", res.getError());
				}
				return success("UtilRefs", ValidationResult.ValidationType.CARDINALITY, "UtilRefs", path, "");
			})
			.collect(toList());
	}

}
