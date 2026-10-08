package com.rosetta.test.model.agreement;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.test.model.agreement.meta.BarMeta;
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Bar", builder=Bar.BarBuilderImpl.class, version="test")
@RuneDataType(value="Bar", model="com", builder=Bar.BarBuilderImpl.class, version="test")
public interface Bar extends RosettaModelObject {

	BarMeta metaData = new BarMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getId();

	/*********************** Build Methods  ***********************/
	Bar build();
	
	Bar.BarBuilder toBuilder();
	
	static Bar.BarBuilder builder() {
		return new Bar.BarBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bar> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bar> getType() {
		return Bar.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("id"), BigDecimal.class, getId(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BarBuilder extends Bar, RosettaModelObjectBuilder {
		Bar.BarBuilder setId(BigDecimal id);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("id"), BigDecimal.class, getId(), this);
		}
		

		Bar.BarBuilder prune();
	}

	/*********************** Immutable Implementation of Bar  ***********************/
	class BarImpl implements Bar {
		private final BigDecimal id;
		
		protected BarImpl(Bar.BarBuilder builder) {
			this.id = builder.getId();
		}
		
		@Override
		@RosettaAttribute("id")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("id")
		public BigDecimal getId() {
			return id;
		}
		
		@Override
		public Bar build() {
			return this;
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			Bar.BarBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bar.BarBuilder builder) {
			ofNullable(getId()).ifPresent(builder::setId);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(id, _that.getId())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (id != null ? id.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bar {" +
				"id=" + this.id +
			'}';
		}
	}

	/*********************** Builder Implementation of Bar  ***********************/
	class BarBuilderImpl implements Bar.BarBuilder {
	
		protected BigDecimal id;
		
		@Override
		@RosettaAttribute("id")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("id")
		public BigDecimal getId() {
			return id;
		}
		
		@RosettaAttribute("id")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("id")
		@Override
		public Bar.BarBuilder setId(BigDecimal _id) {
			this.id = _id == null ? null : _id;
			return this;
		}
		
		@Override
		public Bar build() {
			return new Bar.BarImpl(this);
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getId()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bar.BarBuilder o = (Bar.BarBuilder) other;
			
			
			merger.mergeBasic(getId(), o.getId(), this::setId);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(id, _that.getId())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (id != null ? id.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BarBuilder {" +
				"id=" + this.id +
			'}';
		}
	}
}
