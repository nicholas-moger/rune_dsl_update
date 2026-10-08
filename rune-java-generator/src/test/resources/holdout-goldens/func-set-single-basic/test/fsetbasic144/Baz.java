package test.fsetbasic144;

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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.fsetbasic144.meta.BazMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Baz", builder=Baz.BazBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Baz", model="test", builder=Baz.BazBuilderImpl.class, version="0.0.0")
public interface Baz extends RosettaModelObject {

	BazMeta metaData = new BazMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getAttrList();

	/*********************** Build Methods  ***********************/
	Baz build();
	
	Baz.BazBuilder toBuilder();
	
	static Baz.BazBuilder builder() {
		return new Baz.BazBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Baz> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Baz> getType() {
		return Baz.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("attrList"), String.class, getAttrList(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BazBuilder extends Baz, RosettaModelObjectBuilder {
		Baz.BazBuilder addAttrList(String attrList);
		Baz.BazBuilder addAttrList(String attrList, int idx);
		Baz.BazBuilder addAttrList(List<String> attrList);
		Baz.BazBuilder setAttrList(List<String> attrList);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("attrList"), String.class, getAttrList(), this);
		}
		

		Baz.BazBuilder prune();
	}

	/*********************** Immutable Implementation of Baz  ***********************/
	class BazImpl implements Baz {
		private final List<String> attrList;
		
		protected BazImpl(Baz.BazBuilder builder) {
			this.attrList = ofNullable(builder.getAttrList()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("attrList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attrList")
		public List<String> getAttrList() {
			return attrList;
		}
		
		@Override
		public Baz build() {
			return this;
		}
		
		@Override
		public Baz.BazBuilder toBuilder() {
			Baz.BazBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Baz.BazBuilder builder) {
			ofNullable(getAttrList()).ifPresent(builder::setAttrList);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attrList, _that.getAttrList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrList != null ? attrList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Baz {" +
				"attrList=" + this.attrList +
			'}';
		}
	}

	/*********************** Builder Implementation of Baz  ***********************/
	class BazBuilderImpl implements Baz.BazBuilder {
	
		protected List<String> attrList = new ArrayList<>();
		
		@Override
		@RosettaAttribute("attrList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attrList")
		public List<String> getAttrList() {
			return attrList;
		}
		
		@RosettaAttribute("attrList")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attrList")
		@Override
		public Baz.BazBuilder addAttrList(String _attrList) {
			if (_attrList != null) {
				this.attrList.add(_attrList);
			}
			return this;
		}
		
		@Override
		public Baz.BazBuilder addAttrList(String _attrList, int idx) {
			getIndex(this.attrList, idx, () -> _attrList);
			return this;
		}
		
		@Override
		public Baz.BazBuilder addAttrList(List<String> attrLists) {
			if (attrLists != null) {
				for (final String toAdd : attrLists) {
					this.attrList.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("attrList")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("attrList")
		@Override
		public Baz.BazBuilder setAttrList(List<String> attrLists) {
			if (attrLists == null) {
				this.attrList = new ArrayList<>();
			} else {
				this.attrList = attrLists.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Baz build() {
			return new Baz.BazImpl(this);
		}
		
		@Override
		public Baz.BazBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Baz.BazBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttrList()!=null && !getAttrList().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Baz.BazBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Baz.BazBuilder o = (Baz.BazBuilder) other;
			
			
			merger.mergeBasic(getAttrList(), o.getAttrList(), (Consumer<String>) this::addAttrList);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attrList, _that.getAttrList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrList != null ? attrList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BazBuilder {" +
				"attrList=" + this.attrList +
			'}';
		}
	}
}
