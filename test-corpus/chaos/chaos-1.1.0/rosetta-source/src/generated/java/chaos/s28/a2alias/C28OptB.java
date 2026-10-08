package chaos.s28.a2alias;

import chaos.s28.a2alias.meta.C28OptBMeta;
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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option B.
 * @version 1.0.0
 */
@RosettaDataType(value="C28OptB", builder=C28OptB.C28OptBBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28OptB", model="chaos", builder=C28OptB.C28OptBBuilderImpl.class, version="1.0.0")
public interface C28OptB extends RosettaModelObject {

	C28OptBMeta metaData = new C28OptBMeta();

	/*********************** Getter Methods  ***********************/
	String getBv();

	/*********************** Build Methods  ***********************/
	C28OptB build();
	
	C28OptB.C28OptBBuilder toBuilder();
	
	static C28OptB.C28OptBBuilder builder() {
		return new C28OptB.C28OptBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28OptB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28OptB> getType() {
		return C28OptB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("bv"), String.class, getBv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28OptBBuilder extends C28OptB, RosettaModelObjectBuilder {
		C28OptB.C28OptBBuilder setBv(String bv);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("bv"), String.class, getBv(), this);
		}
		

		C28OptB.C28OptBBuilder prune();
	}

	/*********************** Immutable Implementation of C28OptB  ***********************/
	class C28OptBImpl implements C28OptB {
		private final String bv;
		
		protected C28OptBImpl(C28OptB.C28OptBBuilder builder) {
			this.bv = builder.getBv();
		}
		
		@Override
		@RosettaAttribute("bv")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bv")
		public String getBv() {
			return bv;
		}
		
		@Override
		public C28OptB build() {
			return this;
		}
		
		@Override
		public C28OptB.C28OptBBuilder toBuilder() {
			C28OptB.C28OptBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28OptB.C28OptBBuilder builder) {
			ofNullable(getBv()).ifPresent(builder::setBv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28OptB _that = getType().cast(o);
		
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
			return "C28OptB {" +
				"bv=" + this.bv +
			'}';
		}
	}

	/*********************** Builder Implementation of C28OptB  ***********************/
	class C28OptBBuilderImpl implements C28OptB.C28OptBBuilder {
	
		protected String bv;
		
		@Override
		@RosettaAttribute("bv")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bv")
		public String getBv() {
			return bv;
		}
		
		@RosettaAttribute("bv")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bv")
		@Override
		public C28OptB.C28OptBBuilder setBv(String _bv) {
			this.bv = _bv == null ? null : _bv;
			return this;
		}
		
		@Override
		public C28OptB build() {
			return new C28OptB.C28OptBImpl(this);
		}
		
		@Override
		public C28OptB.C28OptBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28OptB.C28OptBBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28OptB.C28OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28OptB.C28OptBBuilder o = (C28OptB.C28OptBBuilder) other;
			
			
			merger.mergeBasic(getBv(), o.getBv(), this::setBv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28OptB _that = getType().cast(o);
		
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
			return "C28OptBBuilder {" +
				"bv=" + this.bv +
			'}';
		}
	}
}
