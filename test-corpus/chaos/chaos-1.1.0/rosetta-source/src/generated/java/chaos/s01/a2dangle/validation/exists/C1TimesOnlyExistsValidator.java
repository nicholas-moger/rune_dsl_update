package chaos.s01.a2dangle.validation.exists;

import chaos.s01.a2dangle.C1Times;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C1TimesOnlyExistsValidator implements ValidatorWithArg<C1Times, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Times> ValidationResult<C1Times> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("d", ExistenceChecker.isSet((Date) o.getD()))
				.put("t", ExistenceChecker.isSet((LocalTime) o.getT()))
				.put("dt", ExistenceChecker.isSet((LocalDateTime) o.getDt()))
				.put("z", ExistenceChecker.isSet((ZonedDateTime) o.getZ()))
				.put("ds", ExistenceChecker.isSet((List<Date>) o.getDs()))
				.put("stamp", ExistenceChecker.isSet((LocalDateTime) o.getStamp()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Times", ValidationResult.ValidationType.ONLY_EXISTS, "C1Times", path, "");
		}
		return failure("C1Times", ValidationResult.ValidationType.ONLY_EXISTS, "C1Times", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
