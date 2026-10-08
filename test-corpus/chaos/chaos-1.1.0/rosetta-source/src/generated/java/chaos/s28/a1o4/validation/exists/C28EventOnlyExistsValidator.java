package chaos.s28.a1o4.validation.exists;

import chaos.s28.a1o4.C28Event;
import chaos.s28.a1o4.C28Extra;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C28EventOnlyExistsValidator implements ValidatorWithArg<C28Event, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C28Event> ValidationResult<C28Event> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("kind", ExistenceChecker.isSet((String) o.getKind()))
				.put("size", ExistenceChecker.isSet((BigDecimal) o.getSize()))
				.put("extra", ExistenceChecker.isSet((C28Extra) o.getExtra()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C28Event", ValidationResult.ValidationType.ONLY_EXISTS, "C28Event", path, "");
		}
		return failure("C28Event", ValidationResult.ValidationType.ONLY_EXISTS, "C28Event", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
