package chaos.s16.a6choice.rival;

import chaos.s16.a6choice.rival.meta.C16RivalOptMeta;
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
 * The rival choice&#39;s own option type.
 * @version 1.0.0
 */
@RosettaDataType(value="C16RivalOpt", builder=C16RivalOpt.C16RivalOptBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16RivalOpt", model="chaos", builder=C16RivalOpt.C16RivalOptBuilderImpl.class, version="1.0.0")
public interface C16RivalOpt extends RosettaModelObject {

	C16RivalOptMeta metaData = new C16RivalOptMeta();

	/*********************** Getter Methods  ***********************/
	String getR();

	/*********************** Build Methods  ***********************/
	C16RivalOpt build();
	
	C16RivalOpt.C16RivalOptBuilder toBuilder();
	
	static C16RivalOpt.C16RivalOptBuilder builder() {
		return new C16RivalOpt.C16RivalOptBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16RivalOpt> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16RivalOpt> getType() {
		return C16RivalOpt.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("r"), String.class, getR(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16RivalOptBuilder extends C16RivalOpt, RosettaModelObjectBuilder {
		C16RivalOpt.C16RivalOptBuilder setR(String r);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("r"), String.class, getR(), this);
		}
		

		C16RivalOpt.C16RivalOptBuilder prune();
	}

	/*********************** Immutable Implementation of C16RivalOpt  ***********************/
	class C16RivalOptImpl implements C16RivalOpt {
		private final String r;
		
		protected C16RivalOptImpl(C16RivalOpt.C16RivalOptBuilder builder) {
			this.r = builder.getR();
		}
		
		@Override
		@RosettaAttribute("r")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("r")
		public String getR() {
			return r;
		}
		
		@Override
		public C16RivalOpt build() {
			return this;
		}
		
		@Override
		public C16RivalOpt.C16RivalOptBuilder toBuilder() {
			C16RivalOpt.C16RivalOptBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16RivalOpt.C16RivalOptBuilder builder) {
			ofNullable(getR()).ifPresent(builder::setR);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16RivalOpt _that = getType().cast(o);
		
			if (!Objects.equals(r, _that.getR())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (r != null ? r.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16RivalOpt {" +
				"r=" + this.r +
			'}';
		}
	}

	/*********************** Builder Implementation of C16RivalOpt  ***********************/
	class C16RivalOptBuilderImpl implements C16RivalOpt.C16RivalOptBuilder {
	
		protected String r;
		
		@Override
		@RosettaAttribute("r")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("r")
		public String getR() {
			return r;
		}
		
		@RosettaAttribute("r")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("r")
		@Override
		public C16RivalOpt.C16RivalOptBuilder setR(String _r) {
			this.r = _r == null ? null : _r;
			return this;
		}
		
		@Override
		public C16RivalOpt build() {
			return new C16RivalOpt.C16RivalOptImpl(this);
		}
		
		@Override
		public C16RivalOpt.C16RivalOptBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16RivalOpt.C16RivalOptBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getR()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16RivalOpt.C16RivalOptBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16RivalOpt.C16RivalOptBuilder o = (C16RivalOpt.C16RivalOptBuilder) other;
			
			
			merger.mergeBasic(getR(), o.getR(), this::setR);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16RivalOpt _that = getType().cast(o);
		
			if (!Objects.equals(r, _that.getR())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (r != null ? r.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16RivalOptBuilder {" +
				"r=" + this.r +
			'}';
		}
	}
}
