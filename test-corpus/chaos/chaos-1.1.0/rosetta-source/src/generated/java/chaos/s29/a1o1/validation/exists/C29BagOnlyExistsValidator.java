package chaos.s29.a1o1.validation.exists;

import chaos.s29.a1o1.C29Bag;
import chaos.s29.a1o1.C29Outer;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C29BagOnlyExistsValidator implements ValidatorWithArg<C29Bag, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29Bag> ValidationResult<C29Bag> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("outer", ExistenceChecker.isSet((C29Outer) o.getOuter()))
				.put("outers", ExistenceChecker.isSet((List<? extends C29Outer>) o.getOuters()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29Bag", ValidationResult.ValidationType.ONLY_EXISTS, "C29Bag", path, "");
		}
		return failure("C29Bag", ValidationResult.ValidationType.ONLY_EXISTS, "C29Bag", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
