package chaos.s01.a2alias;

import chaos.s01.a2alias.meta.C1CondMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
 * Type-level condition seats.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Cond", builder=C1Cond.C1CondBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Cond", model="chaos", builder=C1Cond.C1CondBuilderImpl.class, version="1.0.0")
public interface C1Cond extends RosettaModelObject {

	C1CondMeta metaData = new C1CondMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getLo();
	BigDecimal getHi();

	/*********************** Build Methods  ***********************/
	C1Cond build();
	
	C1Cond.C1CondBuilder toBuilder();
	
	static C1Cond.C1CondBuilder builder() {
		return new C1Cond.C1CondBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Cond> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Cond> getType() {
		return C1Cond.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("lo"), BigDecimal.class, getLo(), this);
		processor.processBasic(path.newSubPath("hi"), BigDecimal.class, getHi(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1CondBuilder extends C1Cond, RosettaModelObjectBuilder {
		C1Cond.C1CondBuilder setLo(BigDecimal lo);
		C1Cond.C1CondBuilder setHi(BigDecimal hi);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("lo"), BigDecimal.class, getLo(), this);
			processor.processBasic(path.newSubPath("hi"), BigDecimal.class, getHi(), this);
		}
		

		C1Cond.C1CondBuilder prune();
	}

	/*********************** Immutable Implementation of C1Cond  ***********************/
	class C1CondImpl implements C1Cond {
		private final BigDecimal lo;
		private final BigDecimal hi;
		
		protected C1CondImpl(C1Cond.C1CondBuilder builder) {
			this.lo = builder.getLo();
			this.hi = builder.getHi();
		}
		
		@Override
		@RosettaAttribute("lo")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("lo")
		public BigDecimal getLo() {
			return lo;
		}
		
		@Override
		@RosettaAttribute("hi")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("hi")
		public BigDecimal getHi() {
			return hi;
		}
		
		@Override
		public C1Cond build() {
			return this;
		}
		
		@Override
		public C1Cond.C1CondBuilder toBuilder() {
			C1Cond.C1CondBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Cond.C1CondBuilder builder) {
			ofNullable(getLo()).ifPresent(builder::setLo);
			ofNullable(getHi()).ifPresent(builder::setHi);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Cond _that = getType().cast(o);
		
			if (!Objects.equals(lo, _that.getLo())) return false;
			if (!Objects.equals(hi, _that.getHi())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lo != null ? lo.hashCode() : 0);
			_result = 31 * _result + (hi != null ? hi.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Cond {" +
				"lo=" + this.lo + ", " +
				"hi=" + this.hi +
			'}';
		}
	}

	/*********************** Builder Implementation of C1Cond  ***********************/
	class C1CondBuilderImpl implements C1Cond.C1CondBuilder {
	
		protected BigDecimal lo;
		protected BigDecimal hi;
		
		@Override
		@RosettaAttribute("lo")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("lo")
		public BigDecimal getLo() {
			return lo;
		}
		
		@Override
		@RosettaAttribute("hi")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("hi")
		public BigDecimal getHi() {
			return hi;
		}
		
		@RosettaAttribute("lo")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("lo")
		@Override
		public C1Cond.C1CondBuilder setLo(BigDecimal _lo) {
			this.lo = _lo == null ? null : _lo;
			return this;
		}
		
		@RosettaAttribute("hi")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("hi")
		@Override
		public C1Cond.C1CondBuilder setHi(BigDecimal _hi) {
			this.hi = _hi == null ? null : _hi;
			return this;
		}
		
		@Override
		public C1Cond build() {
			return new C1Cond.C1CondImpl(this);
		}
		
		@Override
		public C1Cond.C1CondBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Cond.C1CondBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLo()!=null) return true;
			if (getHi()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Cond.C1CondBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C1Cond.C1CondBuilder o = (C1Cond.C1CondBuilder) other;
			
			
			merger.mergeBasic(getLo(), o.getLo(), this::setLo);
			merger.mergeBasic(getHi(), o.getHi(), this::setHi);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Cond _that = getType().cast(o);
		
			if (!Objects.equals(lo, _that.getLo())) return false;
			if (!Objects.equals(hi, _that.getHi())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lo != null ? lo.hashCode() : 0);
			_result = 31 * _result + (hi != null ? hi.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1CondBuilder {" +
				"lo=" + this.lo + ", " +
				"hi=" + this.hi +
			'}';
		}
	}
}
