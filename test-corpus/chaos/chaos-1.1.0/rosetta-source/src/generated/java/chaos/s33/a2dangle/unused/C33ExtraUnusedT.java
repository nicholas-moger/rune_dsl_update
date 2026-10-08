package chaos.s33.a2dangle.unused;

import chaos.s33.a2dangle.unused.meta.C33ExtraUnusedTMeta;
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
@RosettaDataType(value="C33ExtraUnusedT", builder=C33ExtraUnusedT.C33ExtraUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C33ExtraUnusedT", model="chaos", builder=C33ExtraUnusedT.C33ExtraUnusedTBuilderImpl.class, version="1.0.0")
public interface C33ExtraUnusedT extends RosettaModelObject {

	C33ExtraUnusedTMeta metaData = new C33ExtraUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C33ExtraUnusedT build();
	
	C33ExtraUnusedT.C33ExtraUnusedTBuilder toBuilder();
	
	static C33ExtraUnusedT.C33ExtraUnusedTBuilder builder() {
		return new C33ExtraUnusedT.C33ExtraUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C33ExtraUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C33ExtraUnusedT> getType() {
		return C33ExtraUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C33ExtraUnusedTBuilder extends C33ExtraUnusedT, RosettaModelObjectBuilder {
		C33ExtraUnusedT.C33ExtraUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C33ExtraUnusedT.C33ExtraUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C33ExtraUnusedT  ***********************/
	class C33ExtraUnusedTImpl implements C33ExtraUnusedT {
		private final String stub;
		
		protected C33ExtraUnusedTImpl(C33ExtraUnusedT.C33ExtraUnusedTBuilder builder) {
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
		public C33ExtraUnusedT build() {
			return this;
		}
		
		@Override
		public C33ExtraUnusedT.C33ExtraUnusedTBuilder toBuilder() {
			C33ExtraUnusedT.C33ExtraUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C33ExtraUnusedT.C33ExtraUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33ExtraUnusedT _that = getType().cast(o);
		
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
			return "C33ExtraUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C33ExtraUnusedT  ***********************/
	class C33ExtraUnusedTBuilderImpl implements C33ExtraUnusedT.C33ExtraUnusedTBuilder {
	
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
		public C33ExtraUnusedT.C33ExtraUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C33ExtraUnusedT build() {
			return new C33ExtraUnusedT.C33ExtraUnusedTImpl(this);
		}
		
		@Override
		public C33ExtraUnusedT.C33ExtraUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33ExtraUnusedT.C33ExtraUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33ExtraUnusedT.C33ExtraUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C33ExtraUnusedT.C33ExtraUnusedTBuilder o = (C33ExtraUnusedT.C33ExtraUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33ExtraUnusedT _that = getType().cast(o);
		
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
			return "C33ExtraUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
