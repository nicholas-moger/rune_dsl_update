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
import test.chswitchbare.meta.OptBMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 1.0.0
 */
@RosettaDataType(value="OptB", builder=OptB.OptBBuilderImpl.class, version="1.0.0")
@RuneDataType(value="OptB", model="test", builder=OptB.OptBBuilderImpl.class, version="1.0.0")
public interface OptB extends RosettaModelObject {

	OptBMeta metaData = new OptBMeta();

	/*********************** Getter Methods  ***********************/
	String getFieldB();

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
		processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptBBuilder extends OptB, RosettaModelObjectBuilder {
		OptB.OptBBuilder setFieldB(String fieldB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
		}
		

		OptB.OptBBuilder prune();
	}

	/*********************** Immutable Implementation of OptB  ***********************/
	class OptBImpl implements OptB {
		private final String fieldB;
		
		protected OptBImpl(OptB.OptBBuilder builder) {
			this.fieldB = builder.getFieldB();
		}
		
		@Override
		@RosettaAttribute("fieldB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fieldB")
		public String getFieldB() {
			return fieldB;
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
			ofNullable(getFieldB()).ifPresent(builder::setFieldB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptB _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptB {" +
				"fieldB=" + this.fieldB +
			'}';
		}
	}

	/*********************** Builder Implementation of OptB  ***********************/
	class OptBBuilderImpl implements OptB.OptBBuilder {
	
		protected String fieldB;
		
		@Override
		@RosettaAttribute("fieldB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fieldB")
		public String getFieldB() {
			return fieldB;
		}
		
		@RosettaAttribute("fieldB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("fieldB")
		@Override
		public OptB.OptBBuilder setFieldB(String _fieldB) {
			this.fieldB = _fieldB == null ? null : _fieldB;
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
			if (getFieldB()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptB.OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptB.OptBBuilder o = (OptB.OptBBuilder) other;
			
			
			merger.mergeBasic(getFieldB(), o.getFieldB(), this::setFieldB);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptB _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptBBuilder {" +
				"fieldB=" + this.fieldB +
			'}';
		}
	}
}
