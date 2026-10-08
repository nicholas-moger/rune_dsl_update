package test.pojo;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RosettaIgnore;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneIgnore;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.pojo.meta.ChildMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Child", builder=Child.ChildBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Child", model="test", builder=Child.ChildBuilderImpl.class, version="0.0.0")
public interface Child extends Parent {

	ChildMeta metaData = new ChildMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends ReferenceWithMetaString> getAttrOverriddenAsReferenceWithMetaString();

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
		processRosetta(path.newSubPath("attr"), processor, ReferenceWithMetaString.class, getAttrOverriddenAsReferenceWithMetaString());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ChildBuilder extends Child, Parent.ParentBuilder {
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreateAttrOverriddenAsReferenceWithMetaString(int index);
		@Override
		List<? extends ReferenceWithMetaString.ReferenceWithMetaStringBuilder> getAttrOverriddenAsReferenceWithMetaString();
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
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(ReferenceWithMetaString attr);
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(ReferenceWithMetaString attr, int idx);
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(String attr);
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(String attr, int idx);
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(List<? extends ReferenceWithMetaString> attr);
		Child.ChildBuilder setAttrOverriddenAsReferenceWithMetaString(List<? extends ReferenceWithMetaString> attr);
		Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(List<? extends String> attr);
		Child.ChildBuilder setAttrOverriddenAsReferenceWithMetaStringValue(List<? extends String> attr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("attr"), processor, ReferenceWithMetaString.ReferenceWithMetaStringBuilder.class, getAttrOverriddenAsReferenceWithMetaString());
		}
		

		Child.ChildBuilder prune();
	}

	/*********************** Immutable Implementation of Child  ***********************/
	class ChildImpl implements Child {
		private final List<? extends ReferenceWithMetaString> attr;
		
		protected ChildImpl(Child.ChildBuilder builder) {
			this.attr = ofNullable(builder.getAttrOverriddenAsReferenceWithMetaString()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends ReferenceWithMetaString> getAttrOverriddenAsReferenceWithMetaString() {
			return attr;
		}
		
		@Override
		@RosettaIgnore
		@RuneIgnore
		public List<? extends FieldWithMetaString> getAttr() {
			return attr.stream()
				.<FieldWithMetaString>map(referenceWithMetaString -> {
					final String string = referenceWithMetaString.getValue();
					return string == null ? FieldWithMetaString.builder().build() : FieldWithMetaString.builder().setValue(string).build();
				})
				.collect(Collectors.toList())
			;
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
			ofNullable(getAttrOverriddenAsReferenceWithMetaString()).ifPresent(builder::setAttrOverriddenAsReferenceWithMetaString);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Child _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttrOverriddenAsReferenceWithMetaString())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attr != null ? attr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Child {" +
				"attr=" + this.attr +
			'}';
		}
	}

	/*********************** Builder Implementation of Child  ***********************/
	class ChildBuilderImpl implements Child.ChildBuilder {
	
		protected List<ReferenceWithMetaString.ReferenceWithMetaStringBuilder> attr = new ArrayList<>();
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends ReferenceWithMetaString.ReferenceWithMetaStringBuilder> getAttrOverriddenAsReferenceWithMetaString() {
			return attr;
		}
		
		@Override
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreateAttrOverriddenAsReferenceWithMetaString(int index) {
			if (attr==null) {
				this.attr = new ArrayList<>();
			}
			return getIndex(attr, index, () -> {
						ReferenceWithMetaString.ReferenceWithMetaStringBuilder newAttr = ReferenceWithMetaString.builder();
						return newAttr;
					});
		}
		
		@Override
		@RosettaIgnore
		@RuneIgnore
		public List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getAttr() {
			return attr.stream()
				.<FieldWithMetaString>map(referenceWithMetaString -> {
					final String string = referenceWithMetaString.getValue();
					return string == null ? FieldWithMetaString.builder().build() : FieldWithMetaString.builder().setValue(string).build();
				})
				.collect(Collectors.toList())
			.stream().map(fieldWithMetaString -> fieldWithMetaString.toBuilder()).collect(Collectors.toList());
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateAttr(int index) {
			final ReferenceWithMetaString referenceWithMetaString = getOrCreateAttrOverriddenAsReferenceWithMetaString(index);
			if (referenceWithMetaString == null) {
				return FieldWithMetaString.builder().build().toBuilder();
			}
			final String string = referenceWithMetaString.getValue();
			return string == null ? FieldWithMetaString.builder().build().toBuilder() : FieldWithMetaString.builder().setValue(string).build().toBuilder();
		}
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attr")
		@Override
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(ReferenceWithMetaString _attr) {
			if (_attr != null) {
				this.attr.add(_attr.toBuilder());
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(ReferenceWithMetaString _attr, int idx) {
			getIndex(this.attr, idx, () -> _attr.toBuilder());
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(String _attr) {
			this.getOrCreateAttrOverriddenAsReferenceWithMetaString(-1).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(String _attr, int idx) {
			this.getOrCreateAttrOverriddenAsReferenceWithMetaString(idx).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaString(List<? extends ReferenceWithMetaString> attrs) {
			if (attrs != null) {
				for (final ReferenceWithMetaString toAdd : attrs) {
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
		public Child.ChildBuilder setAttrOverriddenAsReferenceWithMetaString(List<? extends ReferenceWithMetaString> attrs) {
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
		public Child.ChildBuilder addAttrOverriddenAsReferenceWithMetaStringValue(List<? extends String> attrs) {
			if (attrs != null) {
				for (final String toAdd : attrs) {
					this.addAttrOverriddenAsReferenceWithMetaStringValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder setAttrOverriddenAsReferenceWithMetaStringValue(List<? extends String> attrs) {
			this.attr.clear();
			if (attrs != null) {
				attrs.forEach(this::addAttrOverriddenAsReferenceWithMetaStringValue);
			}
			return this;
		}
		
		@RosettaIgnore
		@RuneIgnore
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaString _attr) {
			final ReferenceWithMetaString ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = ReferenceWithMetaString.builder().build();
			} else {
				final String string = _attr.getValue();
				ifThenElseResult = string == null ? ReferenceWithMetaString.builder().build() : ReferenceWithMetaString.builder().setValue(string).build();
			}
			return addAttrOverriddenAsReferenceWithMetaString(ifThenElseResult);
		}
		
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaString _attr, int idx) {
			final ReferenceWithMetaString ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = ReferenceWithMetaString.builder().build();
			} else {
				final String string = _attr.getValue();
				ifThenElseResult = string == null ? ReferenceWithMetaString.builder().build() : ReferenceWithMetaString.builder().setValue(string).build();
			}
			return addAttrOverriddenAsReferenceWithMetaString(ifThenElseResult, idx);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(String _attr) {
			return addAttrOverriddenAsReferenceWithMetaStringValue(_attr);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(String _attr, int idx) {
			return addAttrOverriddenAsReferenceWithMetaStringValue(_attr, idx);
		}
		
		@Override
		public Child.ChildBuilder addAttr(List<? extends FieldWithMetaString> attrs) {
			return addAttrOverriddenAsReferenceWithMetaString(attrs.stream()
				.<ReferenceWithMetaString>map(fieldWithMetaString -> {
					final String string = fieldWithMetaString.getValue();
					return string == null ? ReferenceWithMetaString.builder().build() : ReferenceWithMetaString.builder().setValue(string).build();
				})
				.collect(Collectors.toList())
			);
		}
		
		@RosettaIgnore
		@RuneIgnore
		@Override
		public Child.ChildBuilder setAttr(List<? extends FieldWithMetaString> attrs) {
			return setAttrOverriddenAsReferenceWithMetaString(attrs.stream()
				.<ReferenceWithMetaString>map(fieldWithMetaString -> {
					final String string = fieldWithMetaString.getValue();
					return string == null ? ReferenceWithMetaString.builder().build() : ReferenceWithMetaString.builder().setValue(string).build();
				})
				.collect(Collectors.toList())
			);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(List<? extends String> attrs) {
			return addAttrOverriddenAsReferenceWithMetaStringValue(new ArrayList(attrs));
		}
		
		@Override
		public Child.ChildBuilder setAttrValue(List<? extends String> attrs) {
			return setAttrOverriddenAsReferenceWithMetaStringValue(new ArrayList(attrs));
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
			attr = attr.stream().filter(b->b!=null).<ReferenceWithMetaString.ReferenceWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttrOverriddenAsReferenceWithMetaString()!=null && !getAttrOverriddenAsReferenceWithMetaString().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Child.ChildBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Child.ChildBuilder o = (Child.ChildBuilder) other;
			
			merger.mergeRosetta(getAttrOverriddenAsReferenceWithMetaString(), o.getAttrOverriddenAsReferenceWithMetaString(), this::getOrCreateAttrOverriddenAsReferenceWithMetaString);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Child _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttrOverriddenAsReferenceWithMetaString())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attr != null ? attr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ChildBuilder {" +
				"attr=" + this.attr +
			'}';
		}
	}
}
