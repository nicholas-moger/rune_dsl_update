package test.pojo;

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
import com.rosetta.model.metafields.FieldWithMetaBigDecimal;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.pojo.meta.ParentMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Parent", builder=Parent.ParentBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Parent", model="test", builder=Parent.ParentBuilderImpl.class, version="0.0.0")
public interface Parent extends RosettaModelObject {

	ParentMeta metaData = new ParentMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends FieldWithMetaBigDecimal> getAttr();

	/*********************** Build Methods  ***********************/
	Parent build();
	
	Parent.ParentBuilder toBuilder();
	
	static Parent.ParentBuilder builder() {
		return new Parent.ParentBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Parent> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Parent> getType() {
		return Parent.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("attr"), processor, FieldWithMetaBigDecimal.class, getAttr());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ParentBuilder extends Parent, RosettaModelObjectBuilder {
		FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder getOrCreateAttr(int index);
		@Override
		List<? extends FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> getAttr();
		Parent.ParentBuilder addAttr(FieldWithMetaBigDecimal attr);
		Parent.ParentBuilder addAttr(FieldWithMetaBigDecimal attr, int idx);
		Parent.ParentBuilder addAttrValue(BigDecimal attr);
		Parent.ParentBuilder addAttrValue(BigDecimal attr, int idx);
		Parent.ParentBuilder addAttr(List<? extends FieldWithMetaBigDecimal> attr);
		Parent.ParentBuilder setAttr(List<? extends FieldWithMetaBigDecimal> attr);
		Parent.ParentBuilder addAttrValue(List<? extends BigDecimal> attr);
		Parent.ParentBuilder setAttrValue(List<? extends BigDecimal> attr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("attr"), processor, FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder.class, getAttr());
		}
		

		Parent.ParentBuilder prune();
	}

	/*********************** Immutable Implementation of Parent  ***********************/
	class ParentImpl implements Parent {
		private final List<? extends FieldWithMetaBigDecimal> attr;
		
		protected ParentImpl(Parent.ParentBuilder builder) {
			this.attr = ofNullable(builder.getAttr()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends FieldWithMetaBigDecimal> getAttr() {
			return attr;
		}
		
		@Override
		public Parent build() {
			return this;
		}
		
		@Override
		public Parent.ParentBuilder toBuilder() {
			Parent.ParentBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Parent.ParentBuilder builder) {
			ofNullable(getAttr()).ifPresent(builder::setAttr);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Parent _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttr())) return false;
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
			return "Parent {" +
				"attr=" + this.attr +
			'}';
		}
	}

	/*********************** Builder Implementation of Parent  ***********************/
	class ParentBuilderImpl implements Parent.ParentBuilder {
	
		protected List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> attr = new ArrayList<>();
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attr")
		public List<? extends FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> getAttr() {
			return attr;
		}
		
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder getOrCreateAttr(int index) {
			if (attr==null) {
				this.attr = new ArrayList<>();
			}
			return getIndex(attr, index, () -> {
						FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder newAttr = FieldWithMetaBigDecimal.builder();
						return newAttr;
					});
		}
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attr")
		@Override
		public Parent.ParentBuilder addAttr(FieldWithMetaBigDecimal _attr) {
			if (_attr != null) {
				this.attr.add(_attr.toBuilder());
			}
			return this;
		}
		
		@Override
		public Parent.ParentBuilder addAttr(FieldWithMetaBigDecimal _attr, int idx) {
			getIndex(this.attr, idx, () -> _attr.toBuilder());
			return this;
		}
		
		@Override
		public Parent.ParentBuilder addAttrValue(BigDecimal _attr) {
			this.getOrCreateAttr(-1).setValue(_attr);
			return this;
		}
		
		@Override
		public Parent.ParentBuilder addAttrValue(BigDecimal _attr, int idx) {
			this.getOrCreateAttr(idx).setValue(_attr);
			return this;
		}
		
		@Override
		public Parent.ParentBuilder addAttr(List<? extends FieldWithMetaBigDecimal> attrs) {
			if (attrs != null) {
				for (final FieldWithMetaBigDecimal toAdd : attrs) {
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
		public Parent.ParentBuilder setAttr(List<? extends FieldWithMetaBigDecimal> attrs) {
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
		public Parent.ParentBuilder addAttrValue(List<? extends BigDecimal> attrs) {
			if (attrs != null) {
				for (final BigDecimal toAdd : attrs) {
					this.addAttrValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Parent.ParentBuilder setAttrValue(List<? extends BigDecimal> attrs) {
			this.attr.clear();
			if (attrs != null) {
				attrs.forEach(this::addAttrValue);
			}
			return this;
		}
		
		@Override
		public Parent build() {
			return new Parent.ParentImpl(this);
		}
		
		@Override
		public Parent.ParentBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Parent.ParentBuilder prune() {
			attr = attr.stream().filter(b->b!=null).<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttr()!=null && !getAttr().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Parent.ParentBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Parent.ParentBuilder o = (Parent.ParentBuilder) other;
			
			merger.mergeRosetta(getAttr(), o.getAttr(), this::getOrCreateAttr);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Parent _that = getType().cast(o);
		
			if (!ListEquals.listEquals(attr, _that.getAttr())) return false;
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
			return "ParentBuilder {" +
				"attr=" + this.attr +
			'}';
		}
	}
}
