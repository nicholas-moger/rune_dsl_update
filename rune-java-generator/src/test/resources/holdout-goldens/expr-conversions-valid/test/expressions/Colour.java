package test.expressions;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version 0.0.0
 */
@RosettaEnum("Colour")
public enum Colour {

	@RosettaEnumValue(value = "RED") 
	RED("RED", null),
	
	@RosettaEnumValue(value = "BLUE") 
	BLUE("BLUE", null)
;
	private static Map<String, Colour> values;
	static {
        Map<String, Colour> map = new ConcurrentHashMap<>();
		for (Colour instance : Colour.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	Colour(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static Colour fromDisplayName(String name) {
		Colour value = values.get(name);
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
