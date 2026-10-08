package test.pojo;

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
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import test.pojo.meta.ChildMeta;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="Child", builder=Child.ChildBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Child", model="test", builder=Child.ChildBuilderImpl.class, version="0.0.0")
public interface Child extends Parent {

	ChildMeta metaData = new ChildMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	Child build();
	
	Child.ChildBuilder toBuilder();
	
	static Child.ChildBuilder builder() {
		return new Child.ChildBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Child> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Child> getType() {
		return Child.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("attr"), processor, FieldWithMetaString.class, getAttr());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ChildBuilder extends Child, Parent.ParentBuilder {
		@Override
		Child.ChildBuilder addAttr(FieldWithMetaString attr);
		@Override
		Child.ChildBuilder addAttr(FieldWithMetaString attr, int idx);
		@Override
		Child.ChildBuilder addAttrValue(String attr);
		@Override
		Child.ChildBuilder addAttrValue(String attr, int idx);
		@Override
		Child.ChildBuilder addAttr(List<? extends FieldWithMetaString> attr);
		@Override
		Child.ChildBuilder setAttr(List<? extends FieldWithMetaString> attr);
		@Override
		Child.ChildBuilder addAttrValue(List<? extends String> attr);
		@Override
		Child.ChildBuilder setAttrValue(List<? extends String> attr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("attr"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getAttr());
		}
		

		Child.ChildBuilder prune();
	}

	/*********************** Immutable Implementation of Child  ***********************/
	class ChildImpl extends Parent.ParentImpl implements Child {
		
		protected ChildImpl(Child.ChildBuilder builder) {
			super(builder);
		}
		
		@Override
		public Child build() {
			return this;
		}
		
		@Override
		public Child.ChildBuilder toBuilder() {
			Child.ChildBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Child.ChildBuilder builder) {
			super.setBuilderFields(builder);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "Child {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of Child  ***********************/
	class ChildBuilderImpl extends Parent.ParentBuilderImpl implements Child.ChildBuilder {
	
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attr")
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaString _attr) {
			if (_attr != null) {
				this.attr.add(_attr.toBuilder());
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaString _attr, int idx) {
			getIndex(this.attr, idx, () -> _attr.toBuilder());
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(String _attr) {
			this.getOrCreateAttr(-1).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(String _attr, int idx) {
			this.getOrCreateAttr(idx).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttr(List<? extends FieldWithMetaString> attrs) {
			if (attrs != null) {
				for (final FieldWithMetaString toAdd : attrs) {
					this.attr.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("attr")
		@Override
		public Child.ChildBuilder setAttr(List<? extends FieldWithMetaString> attrs) {
			if (attrs == null) {
				this.attr = new ArrayList<>();
			} else {
				this.attr = attrs.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(List<? extends String> attrs) {
			if (attrs != null) {
				for (final String toAdd : attrs) {
					this.addAttrValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder setAttrValue(List<? extends String> attrs) {
			this.attr.clear();
			if (attrs != null) {
				attrs.forEach(this::addAttrValue);
			}
			return this;
		}
		
		@Override
		public Child build() {
			return new Child.ChildImpl(this);
		}
		
		@Override
		public Child.ChildBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Child.ChildBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Child.ChildBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			Child.ChildBuilder o = (Child.ChildBuilder) other;
			
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "ChildBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
