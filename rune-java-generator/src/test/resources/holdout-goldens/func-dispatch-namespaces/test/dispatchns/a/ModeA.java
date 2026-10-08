package test.dispatchns.a;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Dispatch discriminator, namespace a.
 * @version 0.0.0
 */
@RosettaEnum("ModeA")
public enum ModeA {

	@RosettaEnumValue(value = "Fast") 
	FAST("Fast", null),
	
	@RosettaEnumValue(value = "Slow") 
	SLOW("Slow", null)
;
	private static Map<String, ModeA> values;
	static {
        Map<String, ModeA> map = new ConcurrentHashMap<>();
		for (ModeA instance : ModeA.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	ModeA(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static ModeA fromDisplayName(String name) {
		ModeA value = values.get(name);
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
