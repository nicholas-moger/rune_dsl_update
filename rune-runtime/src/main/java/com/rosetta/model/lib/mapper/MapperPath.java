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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;


class MapperPath implements Mapper.Path, Comparable<MapperPath> {

	// Parent-linked immutable node: each hop appends in O(1) and shares its
	// prefix with the parent path (which the mapper items already pin via
	// their parentItem chain, so no lifetime is extended). The flat element
	// view is derived on first read and memoized benign-race: a pure function
	// of the final fields, so a lost race costs a duplicate compute of an
	// identical, safely-publishable value.
	private final MapperPath parent;
	private final PathElement element;
	private final int size;
	private List<PathElement> pathElements;

	MapperPath(MapperPath parent, PathElement element) {
		this.parent = parent;
		this.element = element;
		this.size = parent == null ? 1 : parent.size + 1;
	}

	private List<PathElement> pathElements() {
		List<PathElement> pathElements = this.pathElements;
		if (pathElements == null) {
			PathElement[] elements = new PathElement[size];
			for (MapperPath p = this; p != null; p = p.parent) {
				elements[p.size - 1] = p.element;
			}
			pathElements = Collections.unmodifiableList(Arrays.asList(elements));
			this.pathElements = pathElements;
		}
		return pathElements;
	}

	@Override
	public List<String> getNames() {
		return Collections.unmodifiableList(pathElements().stream()
				.map(PathElement::getName)
				.collect(Collectors.toList()));
	}

	@Override
	public List<String> getGetters() {
		return Collections.unmodifiableList(pathElements().stream()
				.map(PathElement::getGetter)
				.collect(Collectors.toList()));
	}

	@Override
	public String getLastName() {
		return getNames().get(getNames().size() - 1);
	}

	@Override
	public String getFullPath() {
		return String.join("->", pathElements().stream()
				.map(PathElement::getGetterAndContext)
				.collect(Collectors.toList()));
	}

	@Override
	public String toString() {
		return getFullPath();
	}

	public static MapperPathBuilder builder() {
		return new MapperPathBuilder();
	}

	public MapperPathBuilder toBuilder() {
		return new MapperPathBuilder(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		List<PathElement> pathElements = pathElements();
		result = prime * result + ((pathElements == null) ? 0 : pathElements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapperPath other = (MapperPath) obj;
		List<PathElement> pathElements = pathElements();
		List<PathElement> otherPathElements = other.pathElements();
		if (pathElements == null) {
			if (otherPathElements != null)
				return false;
		} else if (!pathElements.equals(otherPathElements))
			return false;
		return true;
	}

	@Override
	public int compareTo(MapperPath other) {
		ListIterator<PathElement> i1 = pathElements().listIterator();
		ListIterator<PathElement> i2 = other.pathElements().listIterator();

		while (i1.hasNext() && i2.hasNext()) {
			int result = i1.next().compareTo(i2.next());
			if(result != 0) {
				return result;
			}
		}
		if(!i1.hasNext() && !i2.hasNext()) {
			return 0;
		}
		else if(i2.hasNext()) {
			return 1;
		}
		else {
			return -1;
		}
	}

	public static class MapperPathBuilder {

		// Single-shot by contract: every add* appends ONE element to the base
		// path and returns the resulting node. Reusing one builder for several
		// add* calls yields INDEPENDENT sibling paths of the same base — it
		// cannot retro-mutate a previously returned path. Every runtime seat
		// constructs a fresh builder per hop.
		private final MapperPath basePath;

		public MapperPathBuilder() {
			this.basePath = null;
		}

		public MapperPathBuilder(MapperPath basePath) {
			this.basePath = basePath;
		}

		public MapperPath addNull() {
			return new MapperPath(basePath, new PathElement("Null"));
		}

		public MapperPath addRoot(Class<?> clazz) {
			String name = clazz.getSimpleName();
			return new MapperPath(basePath, new PathElement(name));
		}

		public MapperPath addFunctionName(String name) {
			return new MapperPath(basePath, new PathElement(name));
		}

		public MapperPath addListFunctionName(String name, int listIndex) {
			return new MapperPath(basePath, new PathElement(name, listIndex));
		}
	}

	private static class PathElement implements Comparable<PathElement> {

		// Display-name conversions keyed by the getter literal, each holding
		// the unchanged guava conversion of that getter. The key population is
		// the get-prefixed name-literal vocabulary generated code passes here —
		// corpus-bounded, two small strings per entry — so no eviction.
		private static final ConcurrentHashMap<String, String> ATTRIBUTE_NAMES = new ConcurrentHashMap<>();

		private final String getter;
		private final Optional<Integer> listIndex;
		// Derived display views, memoized benign-race: pure functions of the
		// final fields (a lost race costs a duplicate compute of the same
		// value), so construction pays neither the conversion nor the format.
		private String name;
		// E.g. getFoo[1]
		private String getterAndContext;

		public PathElement(String getter) {
			this(getter, Optional.empty());
		}

		public PathElement(String getter, int listIndex) {
			this(getter, Optional.of(listIndex));
		}

		private PathElement(String getter, Optional<Integer> listIndex) {
			this.getter = getter;
			this.listIndex = listIndex;
		}

		public String getName() {
			String name = this.name;
			if (name == null) {
				name = toAttributeName(getter);
				this.name = name;
			}
			return name;
		}

		public String getGetter() {
			return getter;
		}

		public String getGetterAndContext() {
			String getterAndContext = this.getterAndContext;
			if (getterAndContext == null) {
				getterAndContext = listIndex
						.map(i -> String.format("%s[%s]", getter, i))
						.orElse(getter);
				this.getterAndContext = getterAndContext;
			}
			return getterAndContext;
		}

		private static String toAttributeName(String getter) {
			if(getter.startsWith("get")) {
				return ATTRIBUTE_NAMES.computeIfAbsent(getter,
						g -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, g.substring(3)));
			}
			return getter;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getter == null) ? 0 : getter.hashCode());
			result = prime * result + ((listIndex == null) ? 0 : listIndex.hashCode());
			String name = getName();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PathElement other = (PathElement) obj;
			if (getter == null) {
				if (other.getter != null)
					return false;
			} else if (!getter.equals(other.getter))
				return false;
			if (listIndex == null) {
				if (other.listIndex != null)
					return false;
			} else if (!listIndex.equals(other.listIndex))
				return false;
			String name = getName();
			String otherName = other.getName();
			if (name == null) {
				if (otherName != null)
					return false;
			} else if (!name.equals(otherName))
				return false;
			return true;
		}

		@Override
		public int compareTo(PathElement other) {
			int nameCompare = getName().compareTo(other.getName());
			if (nameCompare != 0) {
		        return nameCompare;
			} else if (listIndex.isPresent() && other.listIndex.isPresent()) {
	            return listIndex.get().compareTo(other.listIndex.get());
	        } else if (listIndex.isPresent()) {
	            return -1;
	        } else if (other.listIndex.isPresent()) {
	            return 1;
	        } else {
	            return 0;
	        }
		}
	}
}
