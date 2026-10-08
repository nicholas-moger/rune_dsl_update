package test.chswitchedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.chswitchedge.Bag;
import test.chswitchedge.Either;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BagOnlyExistsValidator implements ValidatorWithArg<Bag, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Bag> ValidationResult<Bag> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("eths", ExistenceChecker.isSet((List<? extends Either>) o.getEths()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Bag", ValidationResult.ValidationType.ONLY_EXISTS, "Bag", path, "");
		}
		return failure("Bag", ValidationResult.ValidationType.ONLY_EXISTS, "Bag", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
