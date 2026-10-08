package chaos.s28.a2dangle.validation.exists;

import chaos.s28.a2dangle.C28ChoiceReport;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C28ChoiceReportOnlyExistsValidator implements ValidatorWithArg<C28ChoiceReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C28ChoiceReport> ValidationResult<C28ChoiceReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utiField", ExistenceChecker.isSet((String) o.getUtiField()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C28ChoiceReport", ValidationResult.ValidationType.ONLY_EXISTS, "C28ChoiceReport", path, "");
		}
		return failure("C28ChoiceReport", ValidationResult.ValidationType.ONLY_EXISTS, "C28ChoiceReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
