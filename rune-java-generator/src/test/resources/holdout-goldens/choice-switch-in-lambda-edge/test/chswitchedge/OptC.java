package test.chswitchedge;

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
import java.util.Objects;
import test.chswitchedge.meta.OptCMeta;

import static java.util.Optional.ofNullable;

/**
 * A third option beside the nested choice.
 * @version 0.0.0
 */
@RosettaDataType(value="OptC", builder=OptC.OptCBuilderImpl.class, version="0.0.0")
@RuneDataType(value="OptC", model="test", builder=OptC.OptCBuilderImpl.class, version="0.0.0")
public interface OptC extends RosettaModelObject {

	OptCMeta metaData = new OptCMeta();

	/*********************** Getter Methods  ***********************/
	String getCv();

	/*********************** Build Methods  ***********************/
	OptC build();
	
	OptC.OptCBuilder toBuilder();
	
	static OptC.OptCBuilder builder() {
		return new OptC.OptCBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OptC> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OptC> getType() {
		return OptC.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("cv"), String.class, getCv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptCBuilder extends OptC, RosettaModelObjectBuilder {
		OptC.OptCBuilder setCv(String cv);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("cv"), String.class, getCv(), this);
		}
		

		OptC.OptCBuilder prune();
	}

	/*********************** Immutable Implementation of OptC  ***********************/
	class OptCImpl implements OptC {
		private final String cv;
		
		protected OptCImpl(OptC.OptCBuilder builder) {
			this.cv = builder.getCv();
		}
		
		@Override
		@RosettaAttribute("cv")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("cv")
		public String getCv() {
			return cv;
		}
		
		@Override
		public OptC build() {
			return this;
		}
		
		@Override
		public OptC.OptCBuilder toBuilder() {
			OptC.OptCBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OptC.OptCBuilder builder) {
			ofNullable(getCv()).ifPresent(builder::setCv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptC _that = getType().cast(o);
		
			if (!Objects.equals(cv, _that.getCv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cv != null ? cv.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptC {" +
				"cv=" + this.cv +
			'}';
		}
	}

	/*********************** Builder Implementation of OptC  ***********************/
	class OptCBuilderImpl implements OptC.OptCBuilder {
	
		protected String cv;
		
		@Override
		@RosettaAttribute("cv")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("cv")
		public String getCv() {
			return cv;
		}
		
		@RosettaAttribute("cv")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("cv")
		@Override
		public OptC.OptCBuilder setCv(String _cv) {
			this.cv = _cv == null ? null : _cv;
			return this;
		}
		
		@Override
		public OptC build() {
			return new OptC.OptCImpl(this);
		}
		
		@Override
		public OptC.OptCBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptC.OptCBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptC.OptCBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptC.OptCBuilder o = (OptC.OptCBuilder) other;
			
			
			merger.mergeBasic(getCv(), o.getCv(), this::setCv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptC _that = getType().cast(o);
		
			if (!Objects.equals(cv, _that.getCv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cv != null ? cv.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptCBuilder {" +
				"cv=" + this.cv +
			'}';
		}
	}
}
