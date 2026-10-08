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
import com.rosetta.model.metafields.FieldWithMetaBigDecimal;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
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
	List<? extends FieldWithMetaInteger> getAttrOverriddenAsFieldWithMetaInteger();

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
		processRosetta(path.newSubPath("attr"), processor, FieldWithMetaInteger.class, getAttrOverriddenAsFieldWithMetaInteger());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ChildBuilder extends Child, Parent.ParentBuilder {
		FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateAttrOverriddenAsFieldWithMetaInteger(int index);
		@Override
		List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getAttrOverriddenAsFieldWithMetaInteger();
		@Override
		Child.ChildBuilder addAttr(FieldWithMetaBigDecimal attr);
		@Override
		Child.ChildBuilder addAttr(FieldWithMetaBigDecimal attr, int idx);
		@Override
		Child.ChildBuilder addAttrValue(BigDecimal attr);
		@Override
		Child.ChildBuilder addAttrValue(BigDecimal attr, int idx);
		@Override
		Child.ChildBuilder addAttr(List<? extends FieldWithMetaBigDecimal> attr);
		@Override
		Child.ChildBuilder setAttr(List<? extends FieldWithMetaBigDecimal> attr);
		@Override
		Child.ChildBuilder addAttrValue(List<? extends BigDecimal> attr);
		@Override
		Child.ChildBuilder setAttrValue(List<? extends BigDecimal> attr);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(FieldWithMetaInteger attr);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(FieldWithMetaInteger attr, int idx);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(Integer attr);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(Integer attr, int idx);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(List<? extends FieldWithMetaInteger> attr);
		Child.ChildBuilder setAttrOverriddenAsFieldWithMetaInteger(List<? extends FieldWithMetaInteger> attr);
		Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(List<? extends Integer> attr);
		Child.ChildBuilder setAttrOverriddenAsFieldWithMetaIntegerValue(List<? extends Integer> attr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("attr"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getAttrOverriddenAsFieldWithMetaInteger());
		}
		

		Child.ChildBuilder prune();
	}

	/*********************** Immutable Implementation of Child  ***********************/
	class ChildImpl implements Child {
		private final List<? extends FieldWithMetaInteger> attr;
		
		protected ChildImpl(Child.ChildBuilder builder) {
			this.attr = ofNullable(builder.getAttrOverriddenAsFieldWithMetaInteger()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends FieldWithMetaInteger> getAttrOverriddenAsFieldWithMetaInteger() {
			return attr;
		}
		
		@Override
		@RosettaIgnore
		@RuneIgnore
		public List<? extends FieldWithMetaBigDecimal> getAttr() {
			return attr.stream()
				.<FieldWithMetaBigDecimal>map(fieldWithMetaInteger -> {
					final Integer integer = fieldWithMetaInteger.getValue();
					return integer == null ? FieldWithMetaBigDecimal.builder().build() : FieldWithMetaBigDecimal.builder().setValue(BigDecimal.valueOf(integer)).build();
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
			ofNullable(getAttrOverriddenAsFieldWithMetaInteger()).ifPresent(builder::setAttrOverriddenAsFieldWithMetaInteger);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Child _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttrOverriddenAsFieldWithMetaInteger())) return false;
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
	
		protected List<FieldWithMetaInteger.FieldWithMetaIntegerBuilder> attr = new ArrayList<>();
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getAttrOverriddenAsFieldWithMetaInteger() {
			return attr;
		}
		
		@Override
		public FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateAttrOverriddenAsFieldWithMetaInteger(int index) {
			if (attr==null) {
				this.attr = new ArrayList<>();
			}
			return getIndex(attr, index, () -> {
						FieldWithMetaInteger.FieldWithMetaIntegerBuilder newAttr = FieldWithMetaInteger.builder();
						return newAttr;
					});
		}
		
		@Override
		@RosettaIgnore
		@RuneIgnore
		public List<? extends FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> getAttr() {
			return attr.stream()
				.<FieldWithMetaBigDecimal>map(fieldWithMetaInteger -> {
					final Integer integer = fieldWithMetaInteger.getValue();
					return integer == null ? FieldWithMetaBigDecimal.builder().build() : FieldWithMetaBigDecimal.builder().setValue(BigDecimal.valueOf(integer)).build();
				})
				.collect(Collectors.toList())
			.stream().map(fieldWithMetaBigDecimal -> fieldWithMetaBigDecimal.toBuilder()).collect(Collectors.toList());
		}
		
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder getOrCreateAttr(int index) {
			final FieldWithMetaInteger fieldWithMetaInteger = getOrCreateAttrOverriddenAsFieldWithMetaInteger(index);
			if (fieldWithMetaInteger == null) {
				return FieldWithMetaBigDecimal.builder().build().toBuilder();
			}
			final Integer integer = fieldWithMetaInteger.getValue();
			return integer == null ? FieldWithMetaBigDecimal.builder().build().toBuilder() : FieldWithMetaBigDecimal.builder().setValue(BigDecimal.valueOf(integer)).build().toBuilder();
		}
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attr")
		@Override
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(FieldWithMetaInteger _attr) {
			if (_attr != null) {
				this.attr.add(_attr.toBuilder());
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(FieldWithMetaInteger _attr, int idx) {
			getIndex(this.attr, idx, () -> _attr.toBuilder());
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(Integer _attr) {
			this.getOrCreateAttrOverriddenAsFieldWithMetaInteger(-1).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(Integer _attr, int idx) {
			this.getOrCreateAttrOverriddenAsFieldWithMetaInteger(idx).setValue(_attr);
			return this;
		}
		
		@Override
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaInteger(List<? extends FieldWithMetaInteger> attrs) {
			if (attrs != null) {
				for (final FieldWithMetaInteger toAdd : attrs) {
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
		public Child.ChildBuilder setAttrOverriddenAsFieldWithMetaInteger(List<? extends FieldWithMetaInteger> attrs) {
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
		public Child.ChildBuilder addAttrOverriddenAsFieldWithMetaIntegerValue(List<? extends Integer> attrs) {
			if (attrs != null) {
				for (final Integer toAdd : attrs) {
					this.addAttrOverriddenAsFieldWithMetaIntegerValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Child.ChildBuilder setAttrOverriddenAsFieldWithMetaIntegerValue(List<? extends Integer> attrs) {
			this.attr.clear();
			if (attrs != null) {
				attrs.forEach(this::addAttrOverriddenAsFieldWithMetaIntegerValue);
			}
			return this;
		}
		
		@RosettaIgnore
		@RuneIgnore
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaBigDecimal _attr) {
			final FieldWithMetaInteger ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = FieldWithMetaInteger.builder().build();
			} else {
				final BigDecimal bigDecimal = _attr.getValue();
				if (bigDecimal == null) {
					ifThenElseResult = FieldWithMetaInteger.builder().build();
				} else {
					ifThenElseResult = BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? FieldWithMetaInteger.builder().setValue(bigDecimal.intValue()).build() : FieldWithMetaInteger.builder().setValue(null).build();
				}
			}
			return addAttrOverriddenAsFieldWithMetaInteger(ifThenElseResult);
		}
		
		@Override
		public Child.ChildBuilder addAttr(FieldWithMetaBigDecimal _attr, int idx) {
			final FieldWithMetaInteger ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = FieldWithMetaInteger.builder().build();
			} else {
				final BigDecimal bigDecimal = _attr.getValue();
				if (bigDecimal == null) {
					ifThenElseResult = FieldWithMetaInteger.builder().build();
				} else {
					ifThenElseResult = BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? FieldWithMetaInteger.builder().setValue(bigDecimal.intValue()).build() : FieldWithMetaInteger.builder().setValue(null).build();
				}
			}
			return addAttrOverriddenAsFieldWithMetaInteger(ifThenElseResult, idx);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(BigDecimal _attr) {
			final Integer ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = null;
			} else {
				ifThenElseResult = BigDecimal.valueOf(_attr.intValue()).compareTo(_attr) == 0 ? _attr.intValue() : null;
			}
			return addAttrOverriddenAsFieldWithMetaIntegerValue(ifThenElseResult);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(BigDecimal _attr, int idx) {
			final Integer ifThenElseResult;
			if (_attr == null) {
				ifThenElseResult = null;
			} else {
				ifThenElseResult = BigDecimal.valueOf(_attr.intValue()).compareTo(_attr) == 0 ? _attr.intValue() : null;
			}
			return addAttrOverriddenAsFieldWithMetaIntegerValue(ifThenElseResult, idx);
		}
		
		@Override
		public Child.ChildBuilder addAttr(List<? extends FieldWithMetaBigDecimal> attrs) {
			return addAttrOverriddenAsFieldWithMetaInteger(attrs.stream()
				.<FieldWithMetaInteger>map(fieldWithMetaBigDecimal -> {
					final BigDecimal bigDecimal = fieldWithMetaBigDecimal.getValue();
					if (bigDecimal == null) {
						return FieldWithMetaInteger.builder().build();
					}
					return BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? FieldWithMetaInteger.builder().setValue(bigDecimal.intValue()).build() : FieldWithMetaInteger.builder().setValue(null).build();
				})
				.collect(Collectors.toList())
			);
		}
		
		@RosettaIgnore
		@RuneIgnore
		@Override
		public Child.ChildBuilder setAttr(List<? extends FieldWithMetaBigDecimal> attrs) {
			return setAttrOverriddenAsFieldWithMetaInteger(attrs.stream()
				.<FieldWithMetaInteger>map(fieldWithMetaBigDecimal -> {
					final BigDecimal bigDecimal = fieldWithMetaBigDecimal.getValue();
					if (bigDecimal == null) {
						return FieldWithMetaInteger.builder().build();
					}
					return BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? FieldWithMetaInteger.builder().setValue(bigDecimal.intValue()).build() : FieldWithMetaInteger.builder().setValue(null).build();
				})
				.collect(Collectors.toList())
			);
		}
		
		@Override
		public Child.ChildBuilder addAttrValue(List<? extends BigDecimal> attrs) {
			return addAttrOverriddenAsFieldWithMetaIntegerValue(attrs.stream()
				.<Integer>map(bigDecimal -> BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? bigDecimal.intValue() : null)
				.collect(Collectors.toList())
			);
		}
		
		@Override
		public Child.ChildBuilder setAttrValue(List<? extends BigDecimal> attrs) {
			return setAttrOverriddenAsFieldWithMetaIntegerValue(attrs.stream()
				.<Integer>map(bigDecimal -> BigDecimal.valueOf(bigDecimal.intValue()).compareTo(bigDecimal) == 0 ? bigDecimal.intValue() : null)
				.collect(Collectors.toList())
			);
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
			attr = attr.stream().filter(b->b!=null).<FieldWithMetaInteger.FieldWithMetaIntegerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttrOverriddenAsFieldWithMetaInteger()!=null && !getAttrOverriddenAsFieldWithMetaInteger().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Child.ChildBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Child.ChildBuilder o = (Child.ChildBuilder) other;
			
			merger.mergeRosetta(getAttrOverriddenAsFieldWithMetaInteger(), o.getAttrOverriddenAsFieldWithMetaInteger(), this::getOrCreateAttrOverriddenAsFieldWithMetaInteger);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Child _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttrOverriddenAsFieldWithMetaInteger())) return false;
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
