package test.aliasreserved.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasreserved.PathClash;
import test.aliasreserved.validation.datarule.Path;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class PathClashTypeFormatValidator implements Validator<PathClash> {
	@Inject
	protected Path path1;

	private List<ComparisonResult> getComparisonResults(PathClash o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("pa", o.getPa(), empty(), of(0), empty(), empty()), 
				checkNumber("pas", o.getPas(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path0, PathClash o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(path1.getValidationResults(path0.newSubPath("pa"), o.getPa()));
		final List<Integer> pas = o.getPas();
		if (pas != null) {
			for (int i = 0; i < pas.size(); i++) {
				results.addAll(path1.getValidationResults(path0.newSubPath("pas").withIndex(i), pas.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path0, PathClash o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("PathClash", ValidationResult.ValidationType.TYPE_FORMAT, "PathClash", path0, "", res.getError());
					}
					return success("PathClash", ValidationResult.ValidationType.TYPE_FORMAT, "PathClash", path0, "");
				}),
				runConditions(path0, o).stream()
			)
			.collect(toList());
	}

}
