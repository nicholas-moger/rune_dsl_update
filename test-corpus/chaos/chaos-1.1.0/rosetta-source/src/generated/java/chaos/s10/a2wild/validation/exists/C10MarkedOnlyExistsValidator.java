package chaos.s10.a2wild.validation.exists;

import chaos.s10.a2wild.C10Marked;
import chaos.s10.a2wild.h.C10Aux;
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

public class C10MarkedOnlyExistsValidator implements ValidatorWithArg<C10Marked, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C10Marked> ValidationResult<C10Marked> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("mid", ExistenceChecker.isSet((String) o.getMid()))
				.put("aux", ExistenceChecker.isSet((C10Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C10Marked", ValidationResult.ValidationType.ONLY_EXISTS, "C10Marked", path, "");
		}
		return failure("C10Marked", ValidationResult.ValidationType.ONLY_EXISTS, "C10Marked", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
