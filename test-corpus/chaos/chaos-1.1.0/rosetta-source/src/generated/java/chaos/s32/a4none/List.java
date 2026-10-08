package chaos.s32.a4none;

import chaos.s32.a4none.meta.ListMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A type named List - the model&#39;s List.java beside every sibling POJO&#39;s java.util.List import (the file-scope law at the POJO seat).
 * @version 0.0.0
 */
@RosettaDataType(value="List", builder=List.ListBuilderImpl.class, version="0.0.0")
@RuneDataType(value="List", model="chaos", builder=List.ListBuilderImpl.class, version="0.0.0")
public interface List extends RosettaModelObject {

	ListMeta metaData = new ListMeta();

	/*********************** Getter Methods  ***********************/
	java.util.List<String> getItems();

	/*********************** Build Methods  ***********************/
	List build();
	
	List.ListBuilder toBuilder();
	
	static List.ListBuilder builder() {
		return new List.ListBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends List> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends List> getType() {
		return List.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("items"), String.class, getItems(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ListBuilder extends List, RosettaModelObjectBuilder {
		List.ListBuilder addItems(String items);
		List.ListBuilder addItems(String items, int idx);
		List.ListBuilder addItems(java.util.List<String> items);
		List.ListBuilder setItems(java.util.List<String> items);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("items"), String.class, getItems(), this);
		}
		

		List.ListBuilder prune();
	}

	/*********************** Immutable Implementation of List  ***********************/
	class ListImpl implements List {
		private final java.util.List<String> items;
		
		protected ListImpl(List.ListBuilder builder) {
			this.items = ofNullable(builder.getItems()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("items")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("items")
		public java.util.List<String> getItems() {
			return items;
		}
		
		@Override
		public List build() {
			return this;
		}
		
		@Override
		public List.ListBuilder toBuilder() {
			List.ListBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(List.ListBuilder builder) {
			ofNullable(getItems()).ifPresent(builder::setItems);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			List _that = getType().cast(o);
		
			if (!ListEquals.listEquals(items, _that.getItems())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (items != null ? items.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "List {" +
				"items=" + this.items +
			'}';
		}
	}

	/*********************** Builder Implementation of List  ***********************/
	class ListBuilderImpl implements List.ListBuilder {
	
		protected java.util.List<String> items = new ArrayList<>();
		
		@Override
		@RosettaAttribute("items")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("items")
		public java.util.List<String> getItems() {
			return items;
		}
		
		@RosettaAttribute("items")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("items")
		@Override
		public List.ListBuilder addItems(String _items) {
			if (_items != null) {
				this.items.add(_items);
			}
			return this;
		}
		
		@Override
		public List.ListBuilder addItems(String _items, int idx) {
			getIndex(this.items, idx, () -> _items);
			return this;
		}
		
		@Override
		public List.ListBuilder addItems(java.util.List<String> itemss) {
			if (itemss != null) {
				for (final String toAdd : itemss) {
					this.items.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("items")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("items")
		@Override
		public List.ListBuilder setItems(java.util.List<String> itemss) {
			if (itemss == null) {
				this.items = new ArrayList<>();
			} else {
				this.items = itemss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public List build() {
			return new List.ListImpl(this);
		}
		
		@Override
		public List.ListBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public List.ListBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getItems()!=null && !getItems().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public List.ListBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			List.ListBuilder o = (List.ListBuilder) other;
			
			
			merger.mergeBasic(getItems(), o.getItems(), (Consumer<String>) this::addItems);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			List _that = getType().cast(o);
		
			if (!ListEquals.listEquals(items, _that.getItems())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (items != null ? items.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ListBuilder {" +
				"items=" + this.items +
			'}';
		}
	}
}
