package test.chswitchedge;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.chswitchedge.meta.BothMeta;

import static java.util.Optional.ofNullable;

/**
 * A nested choice - OptA is reachable through Either (the deep option hop).
 * @version 0.0.0
 */
@RosettaDataType(value="Both", builder=Both.BothBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Both", model="test", builder=Both.BothBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Both extends RosettaModelObject {

	BothMeta metaData = new BothMeta();

	/*********************** Getter Methods  ***********************/
	Either getEither();
	OptC getOptC();

	/*********************** Build Methods  ***********************/
	Both build();
	
	Both.BothBuilder toBuilder();
	
	static Both.BothBuilder builder() {
		return new Both.BothBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Both> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Both> getType() {
		return Both.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("Either"), processor, Either.class, getEither());
		processRosetta(path.newSubPath("OptC"), processor, OptC.class, getOptC());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BothBuilder extends Both, RosettaModelObjectBuilder {
		Either.EitherBuilder getOrCreateEither();
		@Override
		Either.EitherBuilder getEither();
		OptC.OptCBuilder getOrCreateOptC();
		@Override
		OptC.OptCBuilder getOptC();
		Both.BothBuilder setEither(Either _Either);
		Both.BothBuilder setOptC(OptC _OptC);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("Either"), processor, Either.EitherBuilder.class, getEither());
			processRosetta(path.newSubPath("OptC"), processor, OptC.OptCBuilder.class, getOptC());
		}
		

		Both.BothBuilder prune();
	}

	/*********************** Immutable Implementation of Both  ***********************/
	class BothImpl implements Both {
		private final Either either;
		private final OptC optC;
		
		protected BothImpl(Both.BothBuilder builder) {
			this.either = ofNullable(builder.getEither()).map(f->f.build()).orElse(null);
			this.optC = ofNullable(builder.getOptC()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("Either")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Either")
		public Either getEither() {
			return either;
		}
		
		@Override
		@RosettaAttribute("OptC")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptC")
		public OptC getOptC() {
			return optC;
		}
		
		@Override
		public Both build() {
			return this;
		}
		
		@Override
		public Both.BothBuilder toBuilder() {
			Both.BothBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Both.BothBuilder builder) {
			ofNullable(getEither()).ifPresent(builder::setEither);
			ofNullable(getOptC()).ifPresent(builder::setOptC);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Both _that = getType().cast(o);
		
			if (!Objects.equals(either, _that.getEither())) return false;
			if (!Objects.equals(optC, _that.getOptC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (either != null ? either.hashCode() : 0);
			_result = 31 * _result + (optC != null ? optC.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Both {" +
				"Either=" + this.either + ", " +
				"OptC=" + this.optC +
			'}';
		}
	}

	/*********************** Builder Implementation of Both  ***********************/
	class BothBuilderImpl implements Both.BothBuilder {
	
		protected Either.EitherBuilder either;
		protected OptC.OptCBuilder optC;
		
		@Override
		@RosettaAttribute("Either")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Either")
		public Either.EitherBuilder getEither() {
			return either;
		}
		
		@Override
		public Either.EitherBuilder getOrCreateEither() {
			Either.EitherBuilder result;
			if (either!=null) {
				result = either;
			}
			else {
				result = either = Either.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("OptC")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptC")
		public OptC.OptCBuilder getOptC() {
			return optC;
		}
		
		@Override
		public OptC.OptCBuilder getOrCreateOptC() {
			OptC.OptCBuilder result;
			if (optC!=null) {
				result = optC;
			}
			else {
				result = optC = OptC.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("Either")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Either")
		@Override
		public Both.BothBuilder setEither(Either _either) {
			this.either = _either == null ? null : _either.toBuilder();
			return this;
		}
		
		@RosettaAttribute("OptC")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptC")
		@Override
		public Both.BothBuilder setOptC(OptC _optC) {
			this.optC = _optC == null ? null : _optC.toBuilder();
			return this;
		}
		
		@Override
		public Both build() {
			return new Both.BothImpl(this);
		}
		
		@Override
		public Both.BothBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Both.BothBuilder prune() {
			if (either!=null && !either.prune().hasData()) either = null;
			if (optC!=null && !optC.prune().hasData()) optC = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEither()!=null && getEither().hasData()) return true;
			if (getOptC()!=null && getOptC().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Both.BothBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Both.BothBuilder o = (Both.BothBuilder) other;
			
			merger.mergeRosetta(getEither(), o.getEither(), this::setEither);
			merger.mergeRosetta(getOptC(), o.getOptC(), this::setOptC);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Both _that = getType().cast(o);
		
			if (!Objects.equals(either, _that.getEither())) return false;
			if (!Objects.equals(optC, _that.getOptC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (either != null ? either.hashCode() : 0);
			_result = 31 * _result + (optC != null ? optC.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BothBuilder {" +
				"Either=" + this.either + ", " +
				"OptC=" + this.optC +
			'}';
		}
	}
}
