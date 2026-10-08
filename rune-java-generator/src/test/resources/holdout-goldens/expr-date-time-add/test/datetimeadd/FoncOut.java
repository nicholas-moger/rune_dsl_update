package test.datetimeadd;

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
import java.time.LocalDateTime;
import java.util.Objects;
import test.datetimeadd.meta.FoncOutMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="FoncOut", builder=FoncOut.FoncOutBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FoncOut", model="test", builder=FoncOut.FoncOutBuilderImpl.class, version="0.0.0")
public interface FoncOut extends RosettaModelObject {

	FoncOutMeta metaData = new FoncOutMeta();

	/*********************** Getter Methods  ***********************/
	LocalDateTime getRes1();
	LocalDateTime getRes2();

	/*********************** Build Methods  ***********************/
	FoncOut build();
	
	FoncOut.FoncOutBuilder toBuilder();
	
	static FoncOut.FoncOutBuilder builder() {
		return new FoncOut.FoncOutBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FoncOut> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FoncOut> getType() {
		return FoncOut.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("res1"), LocalDateTime.class, getRes1(), this);
		processor.processBasic(path.newSubPath("res2"), LocalDateTime.class, getRes2(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FoncOutBuilder extends FoncOut, RosettaModelObjectBuilder {
		FoncOut.FoncOutBuilder setRes1(LocalDateTime res1);
		FoncOut.FoncOutBuilder setRes2(LocalDateTime res2);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("res1"), LocalDateTime.class, getRes1(), this);
			processor.processBasic(path.newSubPath("res2"), LocalDateTime.class, getRes2(), this);
		}
		

		FoncOut.FoncOutBuilder prune();
	}

	/*********************** Immutable Implementation of FoncOut  ***********************/
	class FoncOutImpl implements FoncOut {
		private final LocalDateTime res1;
		private final LocalDateTime res2;
		
		protected FoncOutImpl(FoncOut.FoncOutBuilder builder) {
			this.res1 = builder.getRes1();
			this.res2 = builder.getRes2();
		}
		
		@Override
		@RosettaAttribute("res1")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("res1")
		public LocalDateTime getRes1() {
			return res1;
		}
		
		@Override
		@RosettaAttribute("res2")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("res2")
		public LocalDateTime getRes2() {
			return res2;
		}
		
		@Override
		public FoncOut build() {
			return this;
		}
		
		@Override
		public FoncOut.FoncOutBuilder toBuilder() {
			FoncOut.FoncOutBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FoncOut.FoncOutBuilder builder) {
			ofNullable(getRes1()).ifPresent(builder::setRes1);
			ofNullable(getRes2()).ifPresent(builder::setRes2);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FoncOut _that = getType().cast(o);
		
			if (!Objects.equals(res1, _that.getRes1())) return false;
			if (!Objects.equals(res2, _that.getRes2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (res1 != null ? res1.hashCode() : 0);
			_result = 31 * _result + (res2 != null ? res2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FoncOut {" +
				"res1=" + this.res1 + ", " +
				"res2=" + this.res2 +
			'}';
		}
	}

	/*********************** Builder Implementation of FoncOut  ***********************/
	class FoncOutBuilderImpl implements FoncOut.FoncOutBuilder {
	
		protected LocalDateTime res1;
		protected LocalDateTime res2;
		
		@Override
		@RosettaAttribute("res1")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("res1")
		public LocalDateTime getRes1() {
			return res1;
		}
		
		@Override
		@RosettaAttribute("res2")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("res2")
		public LocalDateTime getRes2() {
			return res2;
		}
		
		@RosettaAttribute("res1")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("res1")
		@Override
		public FoncOut.FoncOutBuilder setRes1(LocalDateTime _res1) {
			this.res1 = _res1 == null ? null : _res1;
			return this;
		}
		
		@RosettaAttribute("res2")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("res2")
		@Override
		public FoncOut.FoncOutBuilder setRes2(LocalDateTime _res2) {
			this.res2 = _res2 == null ? null : _res2;
			return this;
		}
		
		@Override
		public FoncOut build() {
			return new FoncOut.FoncOutImpl(this);
		}
		
		@Override
		public FoncOut.FoncOutBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FoncOut.FoncOutBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRes1()!=null) return true;
			if (getRes2()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FoncOut.FoncOutBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FoncOut.FoncOutBuilder o = (FoncOut.FoncOutBuilder) other;
			
			
			merger.mergeBasic(getRes1(), o.getRes1(), this::setRes1);
			merger.mergeBasic(getRes2(), o.getRes2(), this::setRes2);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FoncOut _that = getType().cast(o);
		
			if (!Objects.equals(res1, _that.getRes1())) return false;
			if (!Objects.equals(res2, _that.getRes2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (res1 != null ? res1.hashCode() : 0);
			_result = 31 * _result + (res2 != null ? res2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FoncOutBuilder {" +
				"res1=" + this.res1 + ", " +
				"res2=" + this.res2 +
			'}';
		}
	}
}
