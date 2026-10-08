package chaos.s32.a2dangle.unused;

import chaos.s32.a2dangle.unused.meta.C32AuxUnusedTMeta;
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
@RosettaDataType(value="C32AuxUnusedT", builder=C32AuxUnusedT.C32AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C32AuxUnusedT", model="chaos", builder=C32AuxUnusedT.C32AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C32AuxUnusedT extends RosettaModelObject {

	C32AuxUnusedTMeta metaData = new C32AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C32AuxUnusedT build();
	
	C32AuxUnusedT.C32AuxUnusedTBuilder toBuilder();
	
	static C32AuxUnusedT.C32AuxUnusedTBuilder builder() {
		return new C32AuxUnusedT.C32AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C32AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C32AuxUnusedT> getType() {
		return C32AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C32AuxUnusedTBuilder extends C32AuxUnusedT, RosettaModelObjectBuilder {
		C32AuxUnusedT.C32AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C32AuxUnusedT.C32AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C32AuxUnusedT  ***********************/
	class C32AuxUnusedTImpl implements C32AuxUnusedT {
		private final String stub;
		
		protected C32AuxUnusedTImpl(C32AuxUnusedT.C32AuxUnusedTBuilder builder) {
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
		public C32AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C32AuxUnusedT.C32AuxUnusedTBuilder toBuilder() {
			C32AuxUnusedT.C32AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C32AuxUnusedT.C32AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32AuxUnusedT _that = getType().cast(o);
		
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
			return "C32AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C32AuxUnusedT  ***********************/
	class C32AuxUnusedTBuilderImpl implements C32AuxUnusedT.C32AuxUnusedTBuilder {
	
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
		public C32AuxUnusedT.C32AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C32AuxUnusedT build() {
			return new C32AuxUnusedT.C32AuxUnusedTImpl(this);
		}
		
		@Override
		public C32AuxUnusedT.C32AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32AuxUnusedT.C32AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32AuxUnusedT.C32AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C32AuxUnusedT.C32AuxUnusedTBuilder o = (C32AuxUnusedT.C32AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32AuxUnusedT _that = getType().cast(o);
		
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
			return "C32AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
