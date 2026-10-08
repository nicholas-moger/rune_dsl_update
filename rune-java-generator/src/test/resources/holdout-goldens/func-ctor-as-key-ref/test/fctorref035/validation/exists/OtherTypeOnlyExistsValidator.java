package test.fctorref035.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.fctorref035.OtherType;
import test.fctorref035.metafields.ReferenceWithMetaTypeWithKey;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class OtherTypeOnlyExistsValidator implements ValidatorWithArg<OtherType, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends OtherType> ValidationResult<OtherType> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("attrSingle", ExistenceChecker.isSet((ReferenceWithMetaTypeWithKey) o.getAttrSingle()))
				.put("attrMulti", ExistenceChecker.isSet((List<? extends ReferenceWithMetaTypeWithKey>) o.getAttrMulti()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("OtherType", ValidationResult.ValidationType.ONLY_EXISTS, "OtherType", path, "");
		}
		return failure("OtherType", ValidationResult.ValidationType.ONLY_EXISTS, "OtherType", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
