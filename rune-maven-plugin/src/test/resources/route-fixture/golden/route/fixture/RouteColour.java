package route.fixture;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * the ENUM pass&#39;s whole population.
 * @version 1.0.0
 */
@RosettaEnum("RouteColour")
public enum RouteColour {

	@RosettaEnumValue(value = "RED") 
	RED("RED", null),
	
	@RosettaEnumValue(value = "BLUE") 
	BLUE("BLUE", null)
;
	private static Map<String, RouteColour> values;
	static {
        Map<String, RouteColour> map = new ConcurrentHashMap<>();
		for (RouteColour instance : RouteColour.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	RouteColour(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static RouteColour fromDisplayName(String name) {
		RouteColour value = values.get(name);
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
