package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.Accessor;
import holdout.typenamedannotations.AccessorType;
import holdout.typenamedannotations.AnnotationRefs;
import holdout.typenamedannotations.Multi;
import holdout.typenamedannotations.Required;
import holdout.typenamedannotations.RosettaAttribute;
import holdout.typenamedannotations.RosettaDataType;
import holdout.typenamedannotations.RosettaMeta;
import holdout.typenamedannotations.RuneAttribute;
import holdout.typenamedannotations.RuneDataType;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AnnotationRefsValidator implements Validator<AnnotationRefs> {

	private List<ComparisonResult> getComparisonResults(AnnotationRefs o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("multi", (Multi) o.getMulti() != null ? 1 : 0, 0, 1), 
				checkCardinality("req", (Required) o.getReq() != null ? 1 : 0, 0, 1), 
				checkCardinality("accessor", (Accessor) o.getAccessor() != null ? 1 : 0, 0, 1), 
				checkCardinality("accessorType", (AccessorType) o.getAccessorType() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaAttribute", (RosettaAttribute) o.getRosettaAttribute() != null ? 1 : 0, 0, 1), 
				checkCardinality("runeAttribute", (RuneAttribute) o.getRuneAttribute() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaDataType", (RosettaDataType) o.getRosettaDataType() != null ? 1 : 0, 0, 1), 
				checkCardinality("runeDataType", (RuneDataType) o.getRuneDataType() != null ? 1 : 0, 0, 1), 
				checkCardinality("rosettaMeta", (RosettaMeta) o.getRosettaMeta() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AnnotationRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AnnotationRefs", ValidationResult.ValidationType.CARDINALITY, "AnnotationRefs", path, "", res.getError());
				}
				return success("AnnotationRefs", ValidationResult.ValidationType.CARDINALITY, "AnnotationRefs", path, "");
			})
			.collect(toList());
	}

}
