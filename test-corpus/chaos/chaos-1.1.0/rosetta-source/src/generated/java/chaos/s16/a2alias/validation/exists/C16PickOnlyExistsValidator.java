package chaos.s16.a2alias.validation.exists;

import chaos.s16.a2alias.C16Alpha;
import chaos.s16.a2alias.C16Beta;
import chaos.s16.a2alias.C16Pick;
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

public class C16PickOnlyExistsValidator implements ValidatorWithArg<C16Pick, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C16Pick> ValidationResult<C16Pick> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C16Alpha", ExistenceChecker.isSet((C16Alpha) o.getC16Alpha()))
				.put("C16Beta", ExistenceChecker.isSet((C16Beta) o.getC16Beta()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C16Pick", ValidationResult.ValidationType.ONLY_EXISTS, "C16Pick", path, "");
		}
		return failure("C16Pick", ValidationResult.ValidationType.ONLY_EXISTS, "C16Pick", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
