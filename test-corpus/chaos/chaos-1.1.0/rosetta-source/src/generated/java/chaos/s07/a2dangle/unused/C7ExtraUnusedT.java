package chaos.s07.a2dangle.unused;

import chaos.s07.a2dangle.unused.meta.C7ExtraUnusedTMeta;
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
 * @version 1.0.0
 */
@RosettaDataType(value="C7ExtraUnusedT", builder=C7ExtraUnusedT.C7ExtraUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C7ExtraUnusedT", model="chaos", builder=C7ExtraUnusedT.C7ExtraUnusedTBuilderImpl.class, version="1.0.0")
public interface C7ExtraUnusedT extends RosettaModelObject {

	C7ExtraUnusedTMeta metaData = new C7ExtraUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C7ExtraUnusedT build();
	
	C7ExtraUnusedT.C7ExtraUnusedTBuilder toBuilder();
	
	static C7ExtraUnusedT.C7ExtraUnusedTBuilder builder() {
		return new C7ExtraUnusedT.C7ExtraUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C7ExtraUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C7ExtraUnusedT> getType() {
		return C7ExtraUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C7ExtraUnusedTBuilder extends C7ExtraUnusedT, RosettaModelObjectBuilder {
		C7ExtraUnusedT.C7ExtraUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C7ExtraUnusedT.C7ExtraUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C7ExtraUnusedT  ***********************/
	class C7ExtraUnusedTImpl implements C7ExtraUnusedT {
		private final String stub;
		
		protected C7ExtraUnusedTImpl(C7ExtraUnusedT.C7ExtraUnusedTBuilder builder) {
			this.stub = builder.getStub();
		}
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@Override
		public C7ExtraUnusedT build() {
			return this;
		}
		
		@Override
		public C7ExtraUnusedT.C7ExtraUnusedTBuilder toBuilder() {
			C7ExtraUnusedT.C7ExtraUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C7ExtraUnusedT.C7ExtraUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7ExtraUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C7ExtraUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C7ExtraUnusedT  ***********************/
	class C7ExtraUnusedTBuilderImpl implements C7ExtraUnusedT.C7ExtraUnusedTBuilder {
	
		protected String stub;
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@RosettaAttribute("stub")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("stub")
		@Override
		public C7ExtraUnusedT.C7ExtraUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C7ExtraUnusedT build() {
			return new C7ExtraUnusedT.C7ExtraUnusedTImpl(this);
		}
		
		@Override
		public C7ExtraUnusedT.C7ExtraUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7ExtraUnusedT.C7ExtraUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7ExtraUnusedT.C7ExtraUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C7ExtraUnusedT.C7ExtraUnusedTBuilder o = (C7ExtraUnusedT.C7ExtraUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7ExtraUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C7ExtraUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
