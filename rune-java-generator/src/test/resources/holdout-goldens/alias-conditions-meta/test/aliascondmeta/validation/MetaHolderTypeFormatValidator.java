package test.aliascondmeta.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.aliascondmeta.MetaHolder;
import test.aliascondmeta.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class MetaHolderTypeFormatValidator implements Validator<MetaHolder> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(MetaHolder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("schemed", o.getSchemed().getValue(), empty(), of(0), empty(), empty()), 
				checkNumber("schemeds", o.getSchemeds().stream().map(FieldWithMetaInteger::getValue).collect(Collectors.toList()), empty(), of(0), empty(), empty()), 
				checkNumber("plain", o.getPlain(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, MetaHolder o) {
		List<ValidationResult<?>> results = new ArrayList();
		final FieldWithMetaInteger fieldWithMetaInteger0 = o.getSchemed();
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("schemed"), (fieldWithMetaInteger0 == null ? null : fieldWithMetaInteger0.getValue())));
		final List<? extends FieldWithMetaInteger> schemeds = o.getSchemeds();
		if (schemeds != null) {
			for (int i = 0; i < schemeds.size(); i++) {
				final FieldWithMetaInteger fieldWithMetaInteger1 = schemeds.get(i);
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("schemeds").withIndex(i), (fieldWithMetaInteger1 == null ? null : fieldWithMetaInteger1.getValue())));
			}
		}
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("plain"), o.getPlain()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaHolder o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("MetaHolder", ValidationResult.ValidationType.TYPE_FORMAT, "MetaHolder", path, "", res.getError());
					}
					return success("MetaHolder", ValidationResult.ValidationType.TYPE_FORMAT, "MetaHolder", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
