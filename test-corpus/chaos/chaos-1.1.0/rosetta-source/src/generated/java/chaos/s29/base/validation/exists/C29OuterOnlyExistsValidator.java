package chaos.s29.base.validation.exists;

import chaos.s29.base.C29Note;
import chaos.s29.base.C29Outer;
import chaos.s29.base.C29Wrap;
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

public class C29OuterOnlyExistsValidator implements ValidatorWithArg<C29Outer, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29Outer> ValidationResult<C29Outer> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C29Note", ExistenceChecker.isSet((C29Note) o.getC29Note()))
				.put("C29Wrap", ExistenceChecker.isSet((C29Wrap) o.getC29Wrap()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29Outer", ValidationResult.ValidationType.ONLY_EXISTS, "C29Outer", path, "");
		}
		return failure("C29Outer", ValidationResult.ValidationType.ONLY_EXISTS, "C29Outer", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
