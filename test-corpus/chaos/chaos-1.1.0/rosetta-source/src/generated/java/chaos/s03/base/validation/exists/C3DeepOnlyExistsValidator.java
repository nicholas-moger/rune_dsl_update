package chaos.s03.base.validation.exists;

import chaos.s03.base.C3Deep;
import chaos.s03.base.C3Inner;
import chaos.s03.base.C3Outer;
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

public class C3DeepOnlyExistsValidator implements ValidatorWithArg<C3Deep, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C3Deep> ValidationResult<C3Deep> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pick", ExistenceChecker.isSet((C3Outer) o.getPick()))
				.put("picks", ExistenceChecker.isSet((List<? extends C3Inner>) o.getPicks()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C3Deep", ValidationResult.ValidationType.ONLY_EXISTS, "C3Deep", path, "");
		}
		return failure("C3Deep", ValidationResult.ValidationType.ONLY_EXISTS, "C3Deep", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
