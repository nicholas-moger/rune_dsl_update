package chaos.s20.a5crlf.validation.exists;

import chaos.s20.a5crlf.C20Branch;
import chaos.s20.a5crlf.C20Trunk;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C20TrunkOnlyExistsValidator implements ValidatorWithArg<C20Trunk, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C20Trunk> ValidationResult<C20Trunk> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("branches", ExistenceChecker.isSet((List<? extends C20Branch>) o.getBranches()))
				.put("title", ExistenceChecker.isSet((String) o.getTitle()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C20Trunk", ValidationResult.ValidationType.ONLY_EXISTS, "C20Trunk", path, "");
		}
		return failure("C20Trunk", ValidationResult.ValidationType.ONLY_EXISTS, "C20Trunk", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
