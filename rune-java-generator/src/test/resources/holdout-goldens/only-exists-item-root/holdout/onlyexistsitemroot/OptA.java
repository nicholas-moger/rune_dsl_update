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
import holdout.onlyexistsitemroot.meta.OptAMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option A.
 * @version 0.0.0
 */
@RosettaDataType(value="OptA", builder=OptA.OptABuilderImpl.class, version="0.0.0")
@RuneDataType(value="OptA", model="holdout", builder=OptA.OptABuilderImpl.class, version="0.0.0")
public interface OptA extends RosettaModelObject {

	OptAMeta metaData = new OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getAv();

	/*********************** Build Methods  ***********************/
	OptA build();
	
	OptA.OptABuilder toBuilder();
	
	static OptA.OptABuilder builder() {
		return new OptA.OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OptA> getType() {
		return OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptABuilder extends OptA, RosettaModelObjectBuilder {
		OptA.OptABuilder setAv(String av);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
		}
		

		OptA.OptABuilder prune();
	}

	/*********************** Immutable Implementation of OptA  ***********************/
	class OptAImpl implements OptA {
		private final String av;
		
		protected OptAImpl(OptA.OptABuilder builder) {
			this.av = builder.getAv();
		}
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@Override
		public OptA build() {
			return this;
		}
		
		@Override
		public OptA.OptABuilder toBuilder() {
			OptA.OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OptA.OptABuilder builder) {
			ofNullable(getAv()).ifPresent(builder::setAv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptA {" +
				"av=" + this.av +
			'}';
		}
	}

	/*********************** Builder Implementation of OptA  ***********************/
	class OptABuilderImpl implements OptA.OptABuilder {
	
		protected String av;
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@RosettaAttribute("av")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("av")
		@Override
		public OptA.OptABuilder setAv(String _av) {
			this.av = _av == null ? null : _av;
			return this;
		}
		
		@Override
		public OptA build() {
			return new OptA.OptAImpl(this);
		}
		
		@Override
		public OptA.OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptA.OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptA.OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptA.OptABuilder o = (OptA.OptABuilder) other;
			
			
			merger.mergeBasic(getAv(), o.getAv(), this::setAv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptABuilder {" +
				"av=" + this.av +
			'}';
		}
	}
}
