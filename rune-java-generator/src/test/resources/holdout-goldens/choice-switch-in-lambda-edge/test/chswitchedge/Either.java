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
import test.chswitchedge.meta.EitherMeta;

import static java.util.Optional.ofNullable;

/**
 * The choice subject (the chaos C18Either).
 * @version 0.0.0
 */
@RosettaDataType(value="Either", builder=Either.EitherBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Either", model="test", builder=Either.EitherBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Either extends RosettaModelObject {

	EitherMeta metaData = new EitherMeta();

	/*********************** Getter Methods  ***********************/
	OptA getOptA();
	OptB getOptB();

	/*********************** Build Methods  ***********************/
	Either build();
	
	Either.EitherBuilder toBuilder();
	
	static Either.EitherBuilder builder() {
		return new Either.EitherBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Either> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Either> getType() {
		return Either.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("OptA"), processor, OptA.class, getOptA());
		processRosetta(path.newSubPath("OptB"), processor, OptB.class, getOptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface EitherBuilder extends Either, RosettaModelObjectBuilder {
		OptA.OptABuilder getOrCreateOptA();
		@Override
		OptA.OptABuilder getOptA();
		OptB.OptBBuilder getOrCreateOptB();
		@Override
		OptB.OptBBuilder getOptB();
		Either.EitherBuilder setOptA(OptA _OptA);
		Either.EitherBuilder setOptB(OptB _OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("OptA"), processor, OptA.OptABuilder.class, getOptA());
			processRosetta(path.newSubPath("OptB"), processor, OptB.OptBBuilder.class, getOptB());
		}
		

		Either.EitherBuilder prune();
	}

	/*********************** Immutable Implementation of Either  ***********************/
	class EitherImpl implements Either {
		private final OptA optA;
		private final OptB optB;
		
		protected EitherImpl(Either.EitherBuilder builder) {
			this.optA = ofNullable(builder.getOptA()).map(f->f.build()).orElse(null);
			this.optB = ofNullable(builder.getOptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptA")
		public OptA getOptA() {
			return optA;
		}
		
		@Override
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptB")
		public OptB getOptB() {
			return optB;
		}
		
		@Override
		public Either build() {
			return this;
		}
		
		@Override
		public Either.EitherBuilder toBuilder() {
			Either.EitherBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Either.EitherBuilder builder) {
			ofNullable(getOptA()).ifPresent(builder::setOptA);
			ofNullable(getOptB()).ifPresent(builder::setOptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Either _that = getType().cast(o);
		
			if (!Objects.equals(optA, _that.getOptA())) return false;
			if (!Objects.equals(optB, _that.getOptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (optA != null ? optA.hashCode() : 0);
			_result = 31 * _result + (optB != null ? optB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Either {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}

	/*********************** Builder Implementation of Either  ***********************/
	class EitherBuilderImpl implements Either.EitherBuilder {
	
		protected OptA.OptABuilder optA;
		protected OptB.OptBBuilder optB;
		
		@Override
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptA")
		public OptA.OptABuilder getOptA() {
			return optA;
		}
		
		@Override
		public OptA.OptABuilder getOrCreateOptA() {
			OptA.OptABuilder result;
			if (optA!=null) {
				result = optA;
			}
			else {
				result = optA = OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptB")
		public OptB.OptBBuilder getOptB() {
			return optB;
		}
		
		@Override
		public OptB.OptBBuilder getOrCreateOptB() {
			OptB.OptBBuilder result;
			if (optB!=null) {
				result = optB;
			}
			else {
				result = optB = OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptA")
		@Override
		public Either.EitherBuilder setOptA(OptA _optA) {
			this.optA = _optA == null ? null : _optA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptB")
		@Override
		public Either.EitherBuilder setOptB(OptB _optB) {
			this.optB = _optB == null ? null : _optB.toBuilder();
			return this;
		}
		
		@Override
		public Either build() {
			return new Either.EitherImpl(this);
		}
		
		@Override
		public Either.EitherBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Either.EitherBuilder prune() {
			if (optA!=null && !optA.prune().hasData()) optA = null;
			if (optB!=null && !optB.prune().hasData()) optB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOptA()!=null && getOptA().hasData()) return true;
			if (getOptB()!=null && getOptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Either.EitherBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Either.EitherBuilder o = (Either.EitherBuilder) other;
			
			merger.mergeRosetta(getOptA(), o.getOptA(), this::setOptA);
			merger.mergeRosetta(getOptB(), o.getOptB(), this::setOptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Either _that = getType().cast(o);
		
			if (!Objects.equals(optA, _that.getOptA())) return false;
			if (!Objects.equals(optB, _that.getOptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (optA != null ? optA.hashCode() : 0);
			_result = 31 * _result + (optB != null ? optB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "EitherBuilder {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}
}
