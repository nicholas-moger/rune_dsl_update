package holdout.typenamedlist.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedlist.HolderJavaFirst;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class HolderJavaFirstOnlyExistsValidator implements ValidatorWithArg<HolderJavaFirst, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends HolderJavaFirst> ValidationResult<HolderJavaFirst> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("names", ExistenceChecker.isSet((List<String>) o.getNames()))
				.put("subject", ExistenceChecker.isSet((holdout.typenamedlist.List) o.getSubject()))
				.put("others", ExistenceChecker.isSet((List<? extends holdout.typenamedlist.List>) o.getOthers()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("HolderJavaFirst", ValidationResult.ValidationType.ONLY_EXISTS, "HolderJavaFirst", path, "");
		}
		return failure("HolderJavaFirst", ValidationResult.ValidationType.ONLY_EXISTS, "HolderJavaFirst", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
