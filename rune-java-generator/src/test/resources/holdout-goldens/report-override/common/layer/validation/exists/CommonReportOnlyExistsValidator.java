package common.layer.validation.exists;

import cde.layer.price.NotationEnum;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import common.layer.CommonReport;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class CommonReportOnlyExistsValidator implements ValidatorWithArg<CommonReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends CommonReport> ValidationResult<CommonReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("notation", ExistenceChecker.isSet((NotationEnum) o.getNotation()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("CommonReport", ValidationResult.ValidationType.ONLY_EXISTS, "CommonReport", path, "");
		}
		return failure("CommonReport", ValidationResult.ValidationType.ONLY_EXISTS, "CommonReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
