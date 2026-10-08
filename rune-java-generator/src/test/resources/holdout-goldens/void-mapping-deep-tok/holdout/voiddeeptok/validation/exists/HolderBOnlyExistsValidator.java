package holdout.voiddeeptok.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.HolderB;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class HolderBOnlyExistsValidator implements ValidatorWithArg<HolderB, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends HolderB> ValidationResult<HolderB> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("tok", ExistenceChecker.isSet((FieldWithMetaVoid) o.getTok()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("HolderB", ValidationResult.ValidationType.ONLY_EXISTS, "HolderB", path, "");
		}
		return failure("HolderB", ValidationResult.ValidationType.ONLY_EXISTS, "HolderB", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
