package test.datesubtract;

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
import com.rosetta.model.lib.records.Date;
import java.util.Objects;
import test.datesubtract.meta.TestMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Test", builder=Test.TestBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Test", model="test", builder=Test.TestBuilderImpl.class, version="0.0.0")
public interface Test extends RosettaModelObject {

	TestMeta metaData = new TestMeta();

	/*********************** Getter Methods  ***********************/
	Date getOne();
	Date getTwo();

	/*********************** Build Methods  ***********************/
	Test build();
	
	Test.TestBuilder toBuilder();
	
	static Test.TestBuilder builder() {
		return new Test.TestBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Test> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Test> getType() {
		return Test.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("one"), Date.class, getOne(), this);
		processor.processBasic(path.newSubPath("two"), Date.class, getTwo(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface TestBuilder extends Test, RosettaModelObjectBuilder {
		Test.TestBuilder setOne(Date one);
		Test.TestBuilder setTwo(Date two);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("one"), Date.class, getOne(), this);
			processor.processBasic(path.newSubPath("two"), Date.class, getTwo(), this);
		}
		

		Test.TestBuilder prune();
	}

	/*********************** Immutable Implementation of Test  ***********************/
	class TestImpl implements Test {
		private final Date one;
		private final Date two;
		
		protected TestImpl(Test.TestBuilder builder) {
			this.one = builder.getOne();
			this.two = builder.getTwo();
		}
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public Date getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("two")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("two")
		public Date getTwo() {
			return two;
		}
		
		@Override
		public Test build() {
			return this;
		}
		
		@Override
		public Test.TestBuilder toBuilder() {
			Test.TestBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Test.TestBuilder builder) {
			ofNullable(getOne()).ifPresent(builder::setOne);
			ofNullable(getTwo()).ifPresent(builder::setTwo);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Test _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(two, _that.getTwo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (two != null ? two.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Test {" +
				"one=" + this.one + ", " +
				"two=" + this.two +
			'}';
		}
	}

	/*********************** Builder Implementation of Test  ***********************/
	class TestBuilderImpl implements Test.TestBuilder {
	
		protected Date one;
		protected Date two;
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public Date getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("two")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("two")
		public Date getTwo() {
			return two;
		}
		
		@RosettaAttribute("one")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("one")
		@Override
		public Test.TestBuilder setOne(Date _one) {
			this.one = _one == null ? null : _one;
			return this;
		}
		
		@RosettaAttribute("two")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("two")
		@Override
		public Test.TestBuilder setTwo(Date _two) {
			this.two = _two == null ? null : _two;
			return this;
		}
		
		@Override
		public Test build() {
			return new Test.TestImpl(this);
		}
		
		@Override
		public Test.TestBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Test.TestBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOne()!=null) return true;
			if (getTwo()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Test.TestBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Test.TestBuilder o = (Test.TestBuilder) other;
			
			
			merger.mergeBasic(getOne(), o.getOne(), this::setOne);
			merger.mergeBasic(getTwo(), o.getTwo(), this::setTwo);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Test _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(two, _that.getTwo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (two != null ? two.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TestBuilder {" +
				"one=" + this.one + ", " +
				"two=" + this.two +
			'}';
		}
	}
}
