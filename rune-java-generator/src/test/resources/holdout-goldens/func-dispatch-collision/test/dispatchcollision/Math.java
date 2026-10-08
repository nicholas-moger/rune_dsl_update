package test.dispatchcollision;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version 0.0.0
 */
@RosettaEnum("Math")
public enum Math {

	@RosettaEnumValue(value = "INCR") 
	INCR("INCR", null),
	
	@RosettaEnumValue(value = "DECR") 
	DECR("DECR", null)
;
	private static Map<String, Math> values;
	static {
        Map<String, Math> map = new ConcurrentHashMap<>();
		for (Math instance : Math.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	Math(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static Math fromDisplayName(String name) {
		Math value = values.get(name);
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
