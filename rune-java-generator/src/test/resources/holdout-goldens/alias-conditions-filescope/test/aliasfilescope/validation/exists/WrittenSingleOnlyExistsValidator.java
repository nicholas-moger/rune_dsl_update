package test.aliasfilescope.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliasfilescope.WrittenSingle;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class WrittenSingleOnlyExistsValidator implements ValidatorWithArg<WrittenSingle, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends WrittenSingle> ValidationResult<WrittenSingle> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("Consumer", ExistenceChecker.isSet((Integer) o.getConsumer()))
				.put("Object", ExistenceChecker.isSet((Integer) o.getObject()))
				.put("Override", ExistenceChecker.isSet((Integer) o.getOverride()))
				.put("Integer", ExistenceChecker.isSet((Integer) o.getInteger()))
				.put("Objects", ExistenceChecker.isSet((Integer) o.getObjects()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("WrittenSingle", ValidationResult.ValidationType.ONLY_EXISTS, "WrittenSingle", path, "");
		}
		return failure("WrittenSingle", ValidationResult.ValidationType.ONLY_EXISTS, "WrittenSingle", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
