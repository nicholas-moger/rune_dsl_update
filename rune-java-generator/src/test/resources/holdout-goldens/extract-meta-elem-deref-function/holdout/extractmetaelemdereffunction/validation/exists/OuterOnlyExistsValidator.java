package holdout.extractmetaelemdereffunction.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.extractmetaelemdereffunction.Outer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class OuterOnlyExistsValidator implements ValidatorWithArg<Outer, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Outer> ValidationResult<Outer> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("code", ExistenceChecker.isSet((FieldWithMetaString) o.getCode()))
				.put("codes", ExistenceChecker.isSet((List<? extends FieldWithMetaString>) o.getCodes()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Outer", ValidationResult.ValidationType.ONLY_EXISTS, "Outer", path, "");
		}
		return failure("Outer", ValidationResult.ValidationType.ONLY_EXISTS, "Outer", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
