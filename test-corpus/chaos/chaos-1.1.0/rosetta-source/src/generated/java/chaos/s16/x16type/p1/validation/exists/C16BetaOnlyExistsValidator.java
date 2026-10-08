package chaos.s16.x16type.p1.validation.exists;

import chaos.s16.x16type.p1.C16Beta;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C16BetaOnlyExistsValidator implements ValidatorWithArg<C16Beta, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C16Beta> ValidationResult<C16Beta> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("b", ExistenceChecker.isSet((String) o.getB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C16Beta", ValidationResult.ValidationType.ONLY_EXISTS, "C16Beta", path, "");
		}
		return failure("C16Beta", ValidationResult.ValidationType.ONLY_EXISTS, "C16Beta", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
