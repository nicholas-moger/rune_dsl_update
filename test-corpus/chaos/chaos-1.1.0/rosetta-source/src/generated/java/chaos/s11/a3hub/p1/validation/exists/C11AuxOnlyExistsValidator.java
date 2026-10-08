package chaos.s11.a3hub.p1.validation.exists;

import chaos.s11.a3hub.p1.C11Aux;
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

public class C11AuxOnlyExistsValidator implements ValidatorWithArg<C11Aux, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C11Aux> ValidationResult<C11Aux> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ax", ExistenceChecker.isSet((String) o.getAx()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C11Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C11Aux", path, "");
		}
		return failure("C11Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C11Aux", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
