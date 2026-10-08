package chaos.s05.x13half.p2;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * to-enum target.
 * @version 1.0.0
 */
@RosettaEnum("C5KindEnum")
public enum C5KindEnum {

	@RosettaEnumValue(value = "Spot") 
	SPOT("Spot", null),
	
	@RosettaEnumValue(value = "Fwd") 
	FWD("Fwd", null)
;
	private static Map<String, C5KindEnum> values;
	static {
        Map<String, C5KindEnum> map = new ConcurrentHashMap<>();
		for (C5KindEnum instance : C5KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C5KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C5KindEnum fromDisplayName(String name) {
		C5KindEnum value = values.get(name);
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
