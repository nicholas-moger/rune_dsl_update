package chaos.s18.a3third.p1;

import chaos.s18.a3third.p1.meta.C18OptBMeta;
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
 * Choice-guard option B.
 * @version 1.0.0
 */
@RosettaDataType(value="C18OptB", builder=C18OptB.C18OptBBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18OptB", model="chaos", builder=C18OptB.C18OptBBuilderImpl.class, version="1.0.0")
public interface C18OptB extends RosettaModelObject {

	C18OptBMeta metaData = new C18OptBMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getBv();

	/*********************** Build Methods  ***********************/
	C18OptB build();
	
	C18OptB.C18OptBBuilder toBuilder();
	
	static C18OptB.C18OptBBuilder builder() {
		return new C18OptB.C18OptBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18OptB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18OptB> getType() {
		return C18OptB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("bv"), BigDecimal.class, getBv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18OptBBuilder extends C18OptB, RosettaModelObjectBuilder {
		C18OptB.C18OptBBuilder setBv(BigDecimal bv);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("bv"), BigDecimal.class, getBv(), this);
		}
		

		C18OptB.C18OptBBuilder prune();
	}

	/*********************** Immutable Implementation of C18OptB  ***********************/
	class C18OptBImpl implements C18OptB {
		private final BigDecimal bv;
		
		protected C18OptBImpl(C18OptB.C18OptBBuilder builder) {
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
		public C18OptB build() {
			return this;
		}
		
		@Override
		public C18OptB.C18OptBBuilder toBuilder() {
			C18OptB.C18OptBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18OptB.C18OptBBuilder builder) {
			ofNullable(getBv()).ifPresent(builder::setBv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18OptB _that = getType().cast(o);
		
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
			return "C18OptB {" +
				"bv=" + this.bv +
			'}';
		}
	}

	/*********************** Builder Implementation of C18OptB  ***********************/
	class C18OptBBuilderImpl implements C18OptB.C18OptBBuilder {
	
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
		public C18OptB.C18OptBBuilder setBv(BigDecimal _bv) {
			this.bv = _bv == null ? null : _bv;
			return this;
		}
		
		@Override
		public C18OptB build() {
			return new C18OptB.C18OptBImpl(this);
		}
		
		@Override
		public C18OptB.C18OptBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18OptB.C18OptBBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18OptB.C18OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18OptB.C18OptBBuilder o = (C18OptB.C18OptBBuilder) other;
			
			
			merger.mergeBasic(getBv(), o.getBv(), this::setBv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18OptB _that = getType().cast(o);
		
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
			return "C18OptBBuilder {" +
				"bv=" + this.bv +
			'}';
		}
	}
}
