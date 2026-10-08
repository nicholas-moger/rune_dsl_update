package test.aliasfilescope.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliasfilescope.Written;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class WrittenOnlyExistsValidator implements ValidatorWithArg<Written, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Written> ValidationResult<Written> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("Object", ExistenceChecker.isSet((List<Integer>) o.getObject()))
				.put("String", ExistenceChecker.isSet((List<Integer>) o.getString()))
				.put("List", ExistenceChecker.isSet((List<Integer>) o.getList()))
				.put("Objects", ExistenceChecker.isSet((List<Integer>) o.getObjects()))
				.put("Consumer", ExistenceChecker.isSet((List<Integer>) o.getConsumer()))
				.put("Collectors", ExistenceChecker.isSet((List<Integer>) o.getCollectors()))
				.put("ImmutableList", ExistenceChecker.isSet((List<Integer>) o.getImmutableList()))
				.put("Processor", ExistenceChecker.isSet((List<Integer>) o.getProcessor()))
				.put("Multi", ExistenceChecker.isSet((List<Integer>) o.getMulti()))
				.put("Override", ExistenceChecker.isSet((List<Integer>) o.getOverride()))
				.put("Lists", ExistenceChecker.isSet((List<Integer>) o.getLists()))
				.put("ValidationResult", ExistenceChecker.isSet((List<Integer>) o.getValidationResult()))
				.put("RosettaPath", ExistenceChecker.isSet((List<Integer>) o.getRosettaPath()))
				.put("Validator", ExistenceChecker.isSet((List<Integer>) o.getValidator()))
				.put("ComparisonResult", ExistenceChecker.isSet((List<Integer>) o.getComparisonResult()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Written", ValidationResult.ValidationType.ONLY_EXISTS, "Written", path, "");
		}
		return failure("Written", ValidationResult.ValidationType.ONLY_EXISTS, "Written", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
