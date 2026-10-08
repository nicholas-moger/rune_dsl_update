package holdout.typenamedlist.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedlist.HolderModelFirst;
import holdout.typenamedlist.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class HolderModelFirstOnlyExistsValidator implements ValidatorWithArg<HolderModelFirst, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends HolderModelFirst> ValidationResult<HolderModelFirst> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("subject", ExistenceChecker.isSet((List) o.getSubject()))
				.put("names", ExistenceChecker.isSet((java.util.List<String>) o.getNames()))
				.put("others", ExistenceChecker.isSet((java.util.List<? extends List>) o.getOthers()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("HolderModelFirst", ValidationResult.ValidationType.ONLY_EXISTS, "HolderModelFirst", path, "");
		}
		return failure("HolderModelFirst", ValidationResult.ValidationType.ONLY_EXISTS, "HolderModelFirst", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
