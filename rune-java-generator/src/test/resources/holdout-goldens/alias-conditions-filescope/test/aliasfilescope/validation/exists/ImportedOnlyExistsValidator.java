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
import test.aliasfilescope.Imported;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class ImportedOnlyExistsValidator implements ValidatorWithArg<Imported, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Imported> ValidationResult<Imported> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("Streams", ExistenceChecker.isSet((List<Integer>) o.getStreams()))
				.put("ArrayList", ExistenceChecker.isSet((List<Integer>) o.getArrayList()))
				.put("Inject", ExistenceChecker.isSet((List<Integer>) o.getInject()))
				.put("Integer", ExistenceChecker.isSet((List<Integer>) o.getInteger()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Imported", ValidationResult.ValidationType.ONLY_EXISTS, "Imported", path, "");
		}
		return failure("Imported", ValidationResult.ValidationType.ONLY_EXISTS, "Imported", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
