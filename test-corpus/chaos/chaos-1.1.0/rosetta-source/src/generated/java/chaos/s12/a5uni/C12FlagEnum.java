package chaos.s12.a5uni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * External-enum mapping target.
 * @version 1.0.0
 */
@RosettaEnum("C12FlagEnum")
public enum C12FlagEnum {

	@RosettaEnumValue(value = "On") 
	ON("On", null),
	
	@RosettaEnumValue(value = "Off") 
	OFF("Off", null)
;
	private static Map<String, C12FlagEnum> values;
	static {
        Map<String, C12FlagEnum> map = new ConcurrentHashMap<>();
		for (C12FlagEnum instance : C12FlagEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C12FlagEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C12FlagEnum fromDisplayName(String name) {
		C12FlagEnum value = values.get(name);
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
