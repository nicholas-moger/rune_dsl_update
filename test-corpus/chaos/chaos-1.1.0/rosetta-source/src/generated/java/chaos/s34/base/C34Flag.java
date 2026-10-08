package chaos.s34.base;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An annotated enum.
 * @version 1.0.0
 */
@RosettaEnum("C34Flag")
public enum C34Flag {

	@RosettaEnumValue(value = "On") 
	ON("On", null),
	
	@RosettaEnumValue(value = "Off") 
	OFF("Off", null)
;
	private static Map<String, C34Flag> values;
	static {
        Map<String, C34Flag> map = new ConcurrentHashMap<>();
		for (C34Flag instance : C34Flag.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C34Flag(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C34Flag fromDisplayName(String name) {
		C34Flag value = values.get(name);
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
