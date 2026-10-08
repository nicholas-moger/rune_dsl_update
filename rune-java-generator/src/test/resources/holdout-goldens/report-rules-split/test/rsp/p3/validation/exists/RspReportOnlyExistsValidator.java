package test.rsp.p3.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.rsp.p3.RspReport;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RspReportOnlyExistsValidator implements ValidatorWithArg<RspReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RspReport> ValidationResult<RspReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utiField", ExistenceChecker.isSet((String) o.getUtiField()))
				.put("notionalField", ExistenceChecker.isSet((BigDecimal) o.getNotionalField()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RspReport", ValidationResult.ValidationType.ONLY_EXISTS, "RspReport", path, "");
		}
		return failure("RspReport", ValidationResult.ValidationType.ONLY_EXISTS, "RspReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
