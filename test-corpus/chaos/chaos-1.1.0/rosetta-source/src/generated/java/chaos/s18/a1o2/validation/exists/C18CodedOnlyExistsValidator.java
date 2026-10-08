package chaos.s18.a1o2.validation.exists;

import chaos.s18.a1o2.C18Coded;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C18CodedOnlyExistsValidator implements ValidatorWithArg<C18Coded, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C18Coded> ValidationResult<C18Coded> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("kind", ExistenceChecker.isSet((FieldWithMetaString) o.getKind()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C18Coded", ValidationResult.ValidationType.ONLY_EXISTS, "C18Coded", path, "");
		}
		return failure("C18Coded", ValidationResult.ValidationType.ONLY_EXISTS, "C18Coded", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
