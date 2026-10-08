package holdout.onlyexistsitemroot;

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
import holdout.onlyexistsitemroot.meta.OptBMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option B.
 * @version 0.0.0
 */
@RosettaDataType(value="OptB", builder=OptB.OptBBuilderImpl.class, version="0.0.0")
@RuneDataType(value="OptB", model="holdout", builder=OptB.OptBBuilderImpl.class, version="0.0.0")
public interface OptB extends RosettaModelObject {

	OptBMeta metaData = new OptBMeta();

	/*********************** Getter Methods  ***********************/
	String getBv();

	/*********************** Build Methods  ***********************/
	OptB build();
	
	OptB.OptBBuilder toBuilder();
	
	static OptB.OptBBuilder builder() {
		return new OptB.OptBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OptB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OptB> getType() {
		return OptB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("bv"), String.class, getBv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptBBuilder extends OptB, RosettaModelObjectBuilder {
		OptB.OptBBuilder setBv(String bv);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("bv"), String.class, getBv(), this);
		}
		

		OptB.OptBBuilder prune();
	}

	/*********************** Immutable Implementation of OptB  ***********************/
	class OptBImpl implements OptB {
		private final String bv;
		
		protected OptBImpl(OptB.OptBBuilder builder) {
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
		public OptB build() {
			return this;
		}
		
		@Override
		public OptB.OptBBuilder toBuilder() {
			OptB.OptBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OptB.OptBBuilder builder) {
			ofNullable(getBv()).ifPresent(builder::setBv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptB _that = getType().cast(o);
		
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
			return "OptB {" +
				"bv=" + this.bv +
			'}';
		}
	}

	/*********************** Builder Implementation of OptB  ***********************/
	class OptBBuilderImpl implements OptB.OptBBuilder {
	
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
		public OptB.OptBBuilder setBv(String _bv) {
			this.bv = _bv == null ? null : _bv;
			return this;
		}
		
		@Override
		public OptB build() {
			return new OptB.OptBImpl(this);
		}
		
		@Override
		public OptB.OptBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptB.OptBBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptB.OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptB.OptBBuilder o = (OptB.OptBBuilder) other;
			
			
			merger.mergeBasic(getBv(), o.getBv(), this::setBv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptB _that = getType().cast(o);
		
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
			return "OptBBuilder {" +
				"bv=" + this.bv +
			'}';
		}
	}
}
