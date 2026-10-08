package chaos.s01.a5uni.validation.exists;

import chaos.s01.a5uni.A5UniProbe;
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

public class A5UniProbeOnlyExistsValidator implements ValidatorWithArg<A5UniProbe, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends A5UniProbe> ValidationResult<A5UniProbe> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("p", ExistenceChecker.isSet((String) o.getP()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("A5UniProbe", ValidationResult.ValidationType.ONLY_EXISTS, "A5UniProbe", path, "");
		}
		return failure("A5UniProbe", ValidationResult.ValidationType.ONLY_EXISTS, "A5UniProbe", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
