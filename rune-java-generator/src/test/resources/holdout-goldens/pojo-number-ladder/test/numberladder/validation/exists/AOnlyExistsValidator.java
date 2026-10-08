package test.numberladder.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.numberladder.A;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AOnlyExistsValidator implements ValidatorWithArg<A, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends A> ValidationResult<A> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("integers", ExistenceChecker.isSet((List<Integer>) o.getIntegers()))
				.put("long", ExistenceChecker.isSet((Long) o.getLong()))
				.put("bigInteger", ExistenceChecker.isSet((BigInteger) o.getBigInteger()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("A", ValidationResult.ValidationType.ONLY_EXISTS, "A", path, "");
		}
		return failure("A", ValidationResult.ValidationType.ONLY_EXISTS, "A", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
