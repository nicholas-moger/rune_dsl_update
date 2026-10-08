package test.dispatchcollision;

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
import test.dispatchcollision.meta.MathInputMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="MathInput", builder=MathInput.MathInputBuilderImpl.class, version="0.0.0")
@RuneDataType(value="MathInput", model="test", builder=MathInput.MathInputBuilderImpl.class, version="0.0.0")
public interface MathInput extends RosettaModelObject {

	MathInputMeta metaData = new MathInputMeta();

	/*********************** Getter Methods  ***********************/
	String getMathInput();
	test.dispatchcollision.Math getMath();

	/*********************** Build Methods  ***********************/
	MathInput build();
	
	MathInput.MathInputBuilder toBuilder();
	
	static MathInput.MathInputBuilder builder() {
		return new MathInput.MathInputBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends MathInput> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends MathInput> getType() {
		return MathInput.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("mathInput"), String.class, getMathInput(), this);
		processor.processBasic(path.newSubPath("math"), test.dispatchcollision.Math.class, getMath(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface MathInputBuilder extends MathInput, RosettaModelObjectBuilder {
		MathInput.MathInputBuilder setMathInput(String mathInput);
		MathInput.MathInputBuilder setMath(test.dispatchcollision.Math math);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("mathInput"), String.class, getMathInput(), this);
			processor.processBasic(path.newSubPath("math"), test.dispatchcollision.Math.class, getMath(), this);
		}
		

		MathInput.MathInputBuilder prune();
	}

	/*********************** Immutable Implementation of MathInput  ***********************/
	class MathInputImpl implements MathInput {
		private final String mathInput;
		private final test.dispatchcollision.Math math;
		
		protected MathInputImpl(MathInput.MathInputBuilder builder) {
			this.mathInput = builder.getMathInput();
			this.math = builder.getMath();
		}
		
		@Override
		@RosettaAttribute("mathInput")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mathInput")
		public String getMathInput() {
			return mathInput;
		}
		
		@Override
		@RosettaAttribute("math")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("math")
		public test.dispatchcollision.Math getMath() {
			return math;
		}
		
		@Override
		public MathInput build() {
			return this;
		}
		
		@Override
		public MathInput.MathInputBuilder toBuilder() {
			MathInput.MathInputBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(MathInput.MathInputBuilder builder) {
			ofNullable(getMathInput()).ifPresent(builder::setMathInput);
			ofNullable(getMath()).ifPresent(builder::setMath);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MathInput _that = getType().cast(o);
		
			if (!Objects.equals(mathInput, _that.getMathInput())) return false;
			if (!Objects.equals(math, _that.getMath())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mathInput != null ? mathInput.hashCode() : 0);
			_result = 31 * _result + (math != null ? math.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MathInput {" +
				"mathInput=" + this.mathInput + ", " +
				"math=" + this.math +
			'}';
		}
	}

	/*********************** Builder Implementation of MathInput  ***********************/
	class MathInputBuilderImpl implements MathInput.MathInputBuilder {
	
		protected String mathInput;
		protected test.dispatchcollision.Math math;
		
		@Override
		@RosettaAttribute("mathInput")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mathInput")
		public String getMathInput() {
			return mathInput;
		}
		
		@Override
		@RosettaAttribute("math")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("math")
		public test.dispatchcollision.Math getMath() {
			return math;
		}
		
		@RosettaAttribute("mathInput")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("mathInput")
		@Override
		public MathInput.MathInputBuilder setMathInput(String _mathInput) {
			this.mathInput = _mathInput == null ? null : _mathInput;
			return this;
		}
		
		@RosettaAttribute("math")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("math")
		@Override
		public MathInput.MathInputBuilder setMath(test.dispatchcollision.Math _math) {
			this.math = _math == null ? null : _math;
			return this;
		}
		
		@Override
		public MathInput build() {
			return new MathInput.MathInputImpl(this);
		}
		
		@Override
		public MathInput.MathInputBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MathInput.MathInputBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMathInput()!=null) return true;
			if (getMath()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MathInput.MathInputBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			MathInput.MathInputBuilder o = (MathInput.MathInputBuilder) other;
			
			
			merger.mergeBasic(getMathInput(), o.getMathInput(), this::setMathInput);
			merger.mergeBasic(getMath(), o.getMath(), this::setMath);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MathInput _that = getType().cast(o);
		
			if (!Objects.equals(mathInput, _that.getMathInput())) return false;
			if (!Objects.equals(math, _that.getMath())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mathInput != null ? mathInput.hashCode() : 0);
			_result = 31 * _result + (math != null ? math.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MathInputBuilder {" +
				"mathInput=" + this.mathInput + ", " +
				"math=" + this.math +
			'}';
		}
	}
}
