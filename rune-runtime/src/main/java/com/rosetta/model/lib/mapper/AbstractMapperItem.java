/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.model.lib.mapper;

import java.util.Optional;

public abstract class AbstractMapperItem<P> implements Comparable<AbstractMapperItem<P>> {

	// Root-seat seed marker for the single-element "Null" path.
	static final Object NULL_PATH_SEED = new Object();

	// The path node chain is built lazily, on first observation, from a
	// package-private SEED stored in place of the built path. The seed is
	// discriminated by runtime type — each shape maps 1:1 onto a producer or
	// sharing seat:
	//   Class<?>           root seat: single-element root of the class's simple
	//                      name (the getSimpleName call is paid at observation,
	//                      not construction)
	//   NULL_PATH_SEED     root seat: the single-element "Null" root
	//   String             hop seat: the parent item's path extended by this
	//                      function-name element; pathListIndex >= 0 selects the
	//                      list-indexed element form. Hop seats always carry the
	//                      parent on the public parentItem channel.
	//   AbstractMapperItem share seat: this item's path IS the source item's
	//                      node (the error-item parent-share and upcast() twin
	//                      shapes) — a package-private ref, NOT the public
	//                      parent channel, which stays exactly as handed in
	//   ChildHopSeed       share seat: the empty-list error corner — the parent
	//                      item's path extended by a function-name element while
	//                      the public parent channel stays empty
	//   MapperPath         a pre-built node handed in directly (the legacy
	//                      constructor shape)
	// The built chain is memoized benign-race: a pure function of final fields,
	// so a lost race builds an identical chain, and path identity is never
	// compared with == anywhere in the runtime. Children build via the parent
	// item's memoized node, so parent-chain instance-sharing survives deferral.
	private final Object pathSeed;
	private final int pathListIndex;
	private final boolean error;
	private final Optional<MapperItem<? extends P, ?>> parentItem;
	private MapperPath path;

	AbstractMapperItem(MapperPath path, boolean error, Optional<MapperItem<? extends P, ?>> parentItem) {
		this(path, -1, error, parentItem);
	}

	AbstractMapperItem(Object pathSeed, int pathListIndex, boolean error, Optional<MapperItem<? extends P, ?>> parentItem) {
		this.pathSeed = pathSeed;
		this.pathListIndex = pathListIndex;
		this.error = error;
		this.parentItem = parentItem;
	}

	public MapperPath getPath() {
		MapperPath path = this.path;
		if (path == null) {
			path = buildPath();
			this.path = path;
		}
		return path;
	}

	private MapperPath buildPath() {
		Object seed = this.pathSeed;
		if (seed == NULL_PATH_SEED) {
			return MapperPath.builder().addNull();
		}
		if (seed instanceof Class) {
			return MapperPath.builder().addRoot((Class<?>) seed);
		}
		if (seed instanceof String) {
			MapperPath parentPath = parentItem
					.orElseThrow(() -> new IllegalStateException("a function-name path seed requires a parent item"))
					.getPath();
			return pathListIndex >= 0
					? parentPath.toBuilder().addListFunctionName((String) seed, pathListIndex)
					: parentPath.toBuilder().addFunctionName((String) seed);
		}
		if (seed instanceof AbstractMapperItem) {
			return ((AbstractMapperItem<?>) seed).getPath();
		}
		if (seed instanceof ChildHopSeed) {
			ChildHopSeed childHop = (ChildHopSeed) seed;
			return childHop.parent.getPath().toBuilder().addFunctionName(childHop.name);
		}
		if (seed instanceof MapperPath) {
			return (MapperPath) seed;
		}
		throw new IllegalStateException("unrecognised path seed "
				+ (seed == null ? "null" : seed.getClass().getName()));
	}

	// The empty-list error corner's seed (MapperItem.getMapperItems): the
	// error item's path extends the PARENT's chain by one hop while its public
	// parent channel stays empty, so the hop cannot ride the parentItem field.
	static final class ChildHopSeed {
		final AbstractMapperItem<?> parent;
		final String name;

		ChildHopSeed(AbstractMapperItem<?> parent, String name) {
			this.parent = parent;
			this.name = name;
		}
	}

	public boolean isError() {
		return error;
	}

	// parent

	public Optional<MapperItem<? extends P, ?>> getParentItem() {
		return parentItem;
	}

	@Override
	public int compareTo(AbstractMapperItem<P> other) {
		return getPath().compareTo(other.getPath());
	}
}
