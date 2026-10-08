package test.reservednames.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.reservednames.FailureMessage;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class FailureMessageOnlyExistsValidator implements ValidatorWithArg<FailureMessage, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends FailureMessage> ValidationResult<FailureMessage> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("text", ExistenceChecker.isSet((String) o.getText()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("FailureMessage", ValidationResult.ValidationType.ONLY_EXISTS, "FailureMessage", path, "");
		}
		return failure("FailureMessage", ValidationResult.ValidationType.ONLY_EXISTS, "FailureMessage", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
