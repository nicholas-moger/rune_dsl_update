package test.deeppath.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.deeppath.Inner;
import test.deeppath.Wrap;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class WrapOnlyExistsValidator implements ValidatorWithArg<Wrap, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Wrap> ValidationResult<Wrap> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("inner", ExistenceChecker.isSet((Inner) o.getInner()))
				.put("text", ExistenceChecker.isSet((String) o.getText()))
				.put("tags", ExistenceChecker.isSet((List<String>) o.getTags()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Wrap", ValidationResult.ValidationType.ONLY_EXISTS, "Wrap", path, "");
		}
		return failure("Wrap", ValidationResult.ValidationType.ONLY_EXISTS, "Wrap", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
