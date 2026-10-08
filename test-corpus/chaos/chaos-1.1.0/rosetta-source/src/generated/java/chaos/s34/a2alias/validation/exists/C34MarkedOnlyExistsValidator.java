package chaos.s34.a2alias.validation.exists;

import chaos.s34.a2alias.C34Marked;
import chaos.s34.a2alias.h.C34Aux;
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

public class C34MarkedOnlyExistsValidator implements ValidatorWithArg<C34Marked, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C34Marked> ValidationResult<C34Marked> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("mid", ExistenceChecker.isSet((String) o.getMid()))
				.put("aux", ExistenceChecker.isSet((C34Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C34Marked", ValidationResult.ValidationType.ONLY_EXISTS, "C34Marked", path, "");
		}
		return failure("C34Marked", ValidationResult.ValidationType.ONLY_EXISTS, "C34Marked", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
