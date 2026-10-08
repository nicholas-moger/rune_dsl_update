package chaos.s03.a1o1.validation.exists;

import chaos.s03.a1o1.C3Note;
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

public class C3NoteOnlyExistsValidator implements ValidatorWithArg<C3Note, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C3Note> ValidationResult<C3Note> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("text", ExistenceChecker.isSet((String) o.getText()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C3Note", ValidationResult.ValidationType.ONLY_EXISTS, "C3Note", path, "");
		}
		return failure("C3Note", ValidationResult.ValidationType.ONLY_EXISTS, "C3Note", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
