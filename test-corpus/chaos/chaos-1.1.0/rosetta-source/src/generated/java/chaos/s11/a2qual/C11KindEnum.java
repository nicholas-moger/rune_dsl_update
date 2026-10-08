package chaos.s11.a2qual;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.model.lib.annotations.RosettaSynonym;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Enum-value synonyms.
 * @version 1.0.0
 */
@RosettaEnum("C11KindEnum")
public enum C11KindEnum {

	@RosettaSynonym(value = "SPOT", source = "C11XML")
	@RosettaEnumValue(value = "Spot") 
	SPOT("Spot", null),
	
	@RosettaSynonym(value = "FORWARD", source = "C11XML")
	@RosettaSynonym(value = "fwd", source = "C11JSON")
	@RosettaEnumValue(value = "Fwd") 
	FWD("Fwd", null)
;
	private static Map<String, C11KindEnum> values;
	static {
        Map<String, C11KindEnum> map = new ConcurrentHashMap<>();
		for (C11KindEnum instance : C11KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C11KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C11KindEnum fromDisplayName(String name) {
		C11KindEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
