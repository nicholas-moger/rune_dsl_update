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
import test.aliasfilescope.WrittenSingle;
import test.aliasfilescope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class WrittenSingleTypeFormatValidator implements Validator<WrittenSingle> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(WrittenSingle o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("Consumer", o.getConsumer(), empty(), of(0), empty(), empty()), 
				checkNumber("Object", o.getObject(), empty(), of(0), empty(), empty()), 
				checkNumber("Override", o.getOverride(), empty(), of(0), empty(), empty()), 
				checkNumber("Integer", o.getInteger(), empty(), of(0), empty(), empty()), 
				checkNumber("Objects", o.getObjects(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, WrittenSingle o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("Consumer"), o.getConsumer()));
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("Object"), o.getObject()));
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("Override"), o.getOverride()));
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("Integer"), o.getInteger()));
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("Objects"), o.getObjects()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, WrittenSingle o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("WrittenSingle", ValidationResult.ValidationType.TYPE_FORMAT, "WrittenSingle", path, "", res.getError());
					}
					return success("WrittenSingle", ValidationResult.ValidationType.TYPE_FORMAT, "WrittenSingle", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
