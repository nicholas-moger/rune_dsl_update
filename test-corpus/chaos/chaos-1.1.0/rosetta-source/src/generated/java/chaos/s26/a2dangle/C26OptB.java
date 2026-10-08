package chaos.s26.a2dangle;

import chaos.s26.a2dangle.meta.C26OptBMeta;
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
 * Choice option B.
 * @version 1.0.0
 */
@RosettaDataType(value="C26OptB", builder=C26OptB.C26OptBBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26OptB", model="chaos", builder=C26OptB.C26OptBBuilderImpl.class, version="1.0.0")
public interface C26OptB extends RosettaModelObject {

	C26OptBMeta metaData = new C26OptBMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getBv();

	/*********************** Build Methods  ***********************/
	C26OptB build();
	
	C26OptB.C26OptBBuilder toBuilder();
	
	static C26OptB.C26OptBBuilder builder() {
		return new C26OptB.C26OptBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26OptB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26OptB> getType() {
		return C26OptB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("bv"), BigDecimal.class, getBv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26OptBBuilder extends C26OptB, RosettaModelObjectBuilder {
		C26OptB.C26OptBBuilder setBv(BigDecimal bv);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("bv"), BigDecimal.class, getBv(), this);
		}
		

		C26OptB.C26OptBBuilder prune();
	}

	/*********************** Immutable Implementation of C26OptB  ***********************/
	class C26OptBImpl implements C26OptB {
		private final BigDecimal bv;
		
		protected C26OptBImpl(C26OptB.C26OptBBuilder builder) {
			this.bv = builder.getBv();
		}
		
		@Override
		@RosettaAttribute("bv")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("bv")
		public BigDecimal getBv() {
			return bv;
		}
		
		@Override
		public C26OptB build() {
			return this;
		}
		
		@Override
		public C26OptB.C26OptBBuilder toBuilder() {
			C26OptB.C26OptBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26OptB.C26OptBBuilder builder) {
			ofNullable(getBv()).ifPresent(builder::setBv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26OptB _that = getType().cast(o);
		
			if (!Objects.equals(bv, _that.getBv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bv != null ? bv.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26OptB {" +
				"bv=" + this.bv +
			'}';
		}
	}

	/*********************** Builder Implementation of C26OptB  ***********************/
	class C26OptBBuilderImpl implements C26OptB.C26OptBBuilder {
	
		protected BigDecimal bv;
		
		@Override
		@RosettaAttribute("bv")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("bv")
		public BigDecimal getBv() {
			return bv;
		}
		
		@RosettaAttribute("bv")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("bv")
		@Override
		public C26OptB.C26OptBBuilder setBv(BigDecimal _bv) {
			this.bv = _bv == null ? null : _bv;
			return this;
		}
		
		@Override
		public C26OptB build() {
			return new C26OptB.C26OptBImpl(this);
		}
		
		@Override
		public C26OptB.C26OptBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26OptB.C26OptBBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26OptB.C26OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26OptB.C26OptBBuilder o = (C26OptB.C26OptBBuilder) other;
			
			
			merger.mergeBasic(getBv(), o.getBv(), this::setBv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26OptB _that = getType().cast(o);
		
			if (!Objects.equals(bv, _that.getBv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bv != null ? bv.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26OptBBuilder {" +
				"bv=" + this.bv +
			'}';
		}
	}
}
