package test.chswitchbare;

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
import test.chswitchbare.meta.OptAMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 1.0.0
 */
@RosettaDataType(value="OptA", builder=OptA.OptABuilderImpl.class, version="1.0.0")
@RuneDataType(value="OptA", model="test", builder=OptA.OptABuilderImpl.class, version="1.0.0")
public interface OptA extends RosettaModelObject {

	OptAMeta metaData = new OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getFieldA();

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
		processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptABuilder extends OptA, RosettaModelObjectBuilder {
		OptA.OptABuilder setFieldA(String fieldA);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
		}
		

		OptA.OptABuilder prune();
	}

	/*********************** Immutable Implementation of OptA  ***********************/
	class OptAImpl implements OptA {
		private final String fieldA;
		
		protected OptAImpl(OptA.OptABuilder builder) {
			this.fieldA = builder.getFieldA();
		}
		
		@Override
		@RosettaAttribute("fieldA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
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
			ofNullable(getFieldA()).ifPresent(builder::setFieldA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptA {" +
				"fieldA=" + this.fieldA +
			'}';
		}
	}

	/*********************** Builder Implementation of OptA  ***********************/
	class OptABuilderImpl implements OptA.OptABuilder {
	
		protected String fieldA;
		
		@Override
		@RosettaAttribute("fieldA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
		}
		
		@RosettaAttribute("fieldA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("fieldA")
		@Override
		public OptA.OptABuilder setFieldA(String _fieldA) {
			this.fieldA = _fieldA == null ? null : _fieldA;
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
			if (getFieldA()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptA.OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptA.OptABuilder o = (OptA.OptABuilder) other;
			
			
			merger.mergeBasic(getFieldA(), o.getFieldA(), this::setFieldA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptABuilder {" +
				"fieldA=" + this.fieldA +
			'}';
		}
	}
}
