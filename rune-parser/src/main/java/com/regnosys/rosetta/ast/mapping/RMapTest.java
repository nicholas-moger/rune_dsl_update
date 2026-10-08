package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

/**
 * Abstract base class for all mapping test alternatives, corresponding
 * to the {@code rosettaMapTest} grammar rule.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link RMapPath} — path presence test</li>
 *   <li>{@link RMapRosettaPath} — rosetta attribute path reference</li>
 *   <li>{@link RMapTestExists} — path exists test</li>
 *   <li>{@link RMapTestAbsent} — path absent test</li>
 *   <li>{@link RMapTestEquality} — path equality/inequality test</li>
 *   <li>{@link RMapTestFunc} — function-based test</li>
 * </ul>
 */
public abstract class RMapTest extends RNode {
}
