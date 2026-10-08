package chaos.s95.base;

import chaos.s95.base.meta.C95ItemMeta;
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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * REFUSAL-PARITY s95 (base only): a DUPLICATE closure-parameter name (`reduce a, a`) - upstream refuses (&#39;Duplicate name.&#39;); the fork yields two RClosureParameter nodes of which parameter(name) returns the first (seat 8 round 1&#39;s banking).
 * @version 1.0.0
 */
@RosettaDataType(value="C95Item", builder=C95Item.C95ItemBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C95Item", model="chaos", builder=C95Item.C95ItemBuilderImpl.class, version="1.0.0")
public interface C95Item extends RosettaModelObject {

	C95ItemMeta metaData = new C95ItemMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getV();

	/*********************** Build Methods  ***********************/
	C95Item build();
	
	C95Item.C95ItemBuilder toBuilder();
	
	static C95Item.C95ItemBuilder builder() {
		return new C95Item.C95ItemBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C95Item> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C95Item> getType() {
		return C95Item.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C95ItemBuilder extends C95Item, RosettaModelObjectBuilder {
		C95Item.C95ItemBuilder setV(BigDecimal v);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
		}
		

		C95Item.C95ItemBuilder prune();
	}

	/*********************** Immutable Implementation of C95Item  ***********************/
	class C95ItemImpl implements C95Item {
		private final BigDecimal v;
		
		protected C95ItemImpl(C95Item.C95ItemBuilder builder) {
			this.v = builder.getV();
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@Override
		public C95Item build() {
			return this;
		}
		
		@Override
		public C95Item.C95ItemBuilder toBuilder() {
			C95Item.C95ItemBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C95Item.C95ItemBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C95Item _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C95Item {" +
				"v=" + this.v +
			'}';
		}
	}

	/*********************** Builder Implementation of C95Item  ***********************/
	class C95ItemBuilderImpl implements C95Item.C95ItemBuilder {
	
		protected BigDecimal v;
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("v")
		@Override
		public C95Item.C95ItemBuilder setV(BigDecimal _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@Override
		public C95Item build() {
			return new C95Item.C95ItemImpl(this);
		}
		
		@Override
		public C95Item.C95ItemBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C95Item.C95ItemBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C95Item.C95ItemBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C95Item.C95ItemBuilder o = (C95Item.C95ItemBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C95Item _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C95ItemBuilder {" +
				"v=" + this.v +
			'}';
		}
	}
}
