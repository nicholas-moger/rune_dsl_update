package chaos.s18.a2wild.validation.exists;

import chaos.s18.a2wild.C18Either;
import chaos.s18.a2wild.C18OptA;
import chaos.s18.a2wild.C18OptB;
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

public class C18EitherOnlyExistsValidator implements ValidatorWithArg<C18Either, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C18Either> ValidationResult<C18Either> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C18OptA", ExistenceChecker.isSet((C18OptA) o.getC18OptA()))
				.put("C18OptB", ExistenceChecker.isSet((C18OptB) o.getC18OptB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C18Either", ValidationResult.ValidationType.ONLY_EXISTS, "C18Either", path, "");
		}
		return failure("C18Either", ValidationResult.ValidationType.ONLY_EXISTS, "C18Either", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
