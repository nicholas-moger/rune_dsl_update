package test.aliasfilescope.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasfilescope.Own;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class OwnTypeFormatValidator implements Validator<Own> {
	@Inject
	protected test.aliasfilescope.validation.datarule.OwnTypeFormatValidator ownTypeFormatValidator;

	private List<ComparisonResult> getComparisonResults(Own o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("v", o.getV(), empty(), of(0), empty(), empty()), 
				checkNumber("vs", o.getVs(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Own o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(ownTypeFormatValidator.getValidationResults(path.newSubPath("v"), o.getV()));
		final List<Integer> vs = o.getVs();
		if (vs != null) {
			for (int i = 0; i < vs.size(); i++) {
				results.addAll(ownTypeFormatValidator.getValidationResults(path.newSubPath("vs").withIndex(i), vs.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Own o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Own", ValidationResult.ValidationType.TYPE_FORMAT, "Own", path, "", res.getError());
					}
					return success("Own", ValidationResult.ValidationType.TYPE_FORMAT, "Own", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
