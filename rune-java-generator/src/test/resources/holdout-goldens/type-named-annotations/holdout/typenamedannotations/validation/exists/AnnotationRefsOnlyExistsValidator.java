package holdout.typenamedannotations.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AnnotationRefsOnlyExistsValidator implements ValidatorWithArg<AnnotationRefs, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends AnnotationRefs> ValidationResult<AnnotationRefs> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("multi", ExistenceChecker.isSet((Multi) o.getMulti()))
				.put("req", ExistenceChecker.isSet((Required) o.getReq()))
				.put("accessor", ExistenceChecker.isSet((Accessor) o.getAccessor()))
				.put("accessorType", ExistenceChecker.isSet((AccessorType) o.getAccessorType()))
				.put("rosettaAttribute", ExistenceChecker.isSet((RosettaAttribute) o.getRosettaAttribute()))
				.put("runeAttribute", ExistenceChecker.isSet((RuneAttribute) o.getRuneAttribute()))
				.put("rosettaDataType", ExistenceChecker.isSet((RosettaDataType) o.getRosettaDataType()))
				.put("runeDataType", ExistenceChecker.isSet((RuneDataType) o.getRuneDataType()))
				.put("rosettaMeta", ExistenceChecker.isSet((RosettaMeta) o.getRosettaMeta()))
				.put("names", ExistenceChecker.isSet((List<String>) o.getNames()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("AnnotationRefs", ValidationResult.ValidationType.ONLY_EXISTS, "AnnotationRefs", path, "");
		}
		return failure("AnnotationRefs", ValidationResult.ValidationType.ONLY_EXISTS, "AnnotationRefs", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
