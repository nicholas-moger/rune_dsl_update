package chaos.s20.a3third.p1.validation.exists;

import chaos.s20.a3third.p1.C20Branch;
import chaos.s20.a3third.p1.C20Twig;
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

public class C20BranchOnlyExistsValidator implements ValidatorWithArg<C20Branch, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C20Branch> ValidationResult<C20Branch> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("twigs", ExistenceChecker.isSet((List<? extends C20Twig>) o.getTwigs()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C20Branch", ValidationResult.ValidationType.ONLY_EXISTS, "C20Branch", path, "");
		}
		return failure("C20Branch", ValidationResult.ValidationType.ONLY_EXISTS, "C20Branch", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
