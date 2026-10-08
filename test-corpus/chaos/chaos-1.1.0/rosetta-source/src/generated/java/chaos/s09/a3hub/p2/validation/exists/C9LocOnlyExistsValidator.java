package chaos.s09.a3hub.p2.validation.exists;

import chaos.s09.a3hub.p2.C9Loc;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C9LocOnlyExistsValidator implements ValidatorWithArg<C9Loc, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C9Loc> ValidationResult<C9Loc> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("spot", ExistenceChecker.isSet((FieldWithMetaString) o.getSpot()))
				.put("ptr", ExistenceChecker.isSet((ReferenceWithMetaString) o.getPtr()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C9Loc", ValidationResult.ValidationType.ONLY_EXISTS, "C9Loc", path, "");
		}
		return failure("C9Loc", ValidationResult.ValidationType.ONLY_EXISTS, "C9Loc", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
