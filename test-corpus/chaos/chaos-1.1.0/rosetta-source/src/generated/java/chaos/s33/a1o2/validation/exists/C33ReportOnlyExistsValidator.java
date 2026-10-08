package chaos.s33.a1o2.validation.exists;

import chaos.s33.a1o2.C33Report;
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

public class C33ReportOnlyExistsValidator implements ValidatorWithArg<C33Report, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C33Report> ValidationResult<C33Report> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utiField", ExistenceChecker.isSet((String) o.getUtiField()))
				.put("litField", ExistenceChecker.isSet((String) o.getLitField()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C33Report", ValidationResult.ValidationType.ONLY_EXISTS, "C33Report", path, "");
		}
		return failure("C33Report", ValidationResult.ValidationType.ONLY_EXISTS, "C33Report", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
