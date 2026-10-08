package chaos.s20.a5uni.validation.exists;

import chaos.s20.a5uni.C20Leaf;
import chaos.s20.a5uni.C20Twig;
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

public class C20TwigOnlyExistsValidator implements ValidatorWithArg<C20Twig, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C20Twig> ValidationResult<C20Twig> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("leaves", ExistenceChecker.isSet((List<? extends C20Leaf>) o.getLeaves()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C20Twig", ValidationResult.ValidationType.ONLY_EXISTS, "C20Twig", path, "");
		}
		return failure("C20Twig", ValidationResult.ValidationType.ONLY_EXISTS, "C20Twig", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
