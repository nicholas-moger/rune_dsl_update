package chaos.s13.a2dangle.unused;

import chaos.s13.a2dangle.unused.meta.C13AuxUnusedTMeta;
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
@RosettaDataType(value="C13AuxUnusedT", builder=C13AuxUnusedT.C13AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C13AuxUnusedT", model="chaos", builder=C13AuxUnusedT.C13AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C13AuxUnusedT extends RosettaModelObject {

	C13AuxUnusedTMeta metaData = new C13AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C13AuxUnusedT build();
	
	C13AuxUnusedT.C13AuxUnusedTBuilder toBuilder();
	
	static C13AuxUnusedT.C13AuxUnusedTBuilder builder() {
		return new C13AuxUnusedT.C13AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C13AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C13AuxUnusedT> getType() {
		return C13AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C13AuxUnusedTBuilder extends C13AuxUnusedT, RosettaModelObjectBuilder {
		C13AuxUnusedT.C13AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C13AuxUnusedT.C13AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C13AuxUnusedT  ***********************/
	class C13AuxUnusedTImpl implements C13AuxUnusedT {
		private final String stub;
		
		protected C13AuxUnusedTImpl(C13AuxUnusedT.C13AuxUnusedTBuilder builder) {
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
		public C13AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C13AuxUnusedT.C13AuxUnusedTBuilder toBuilder() {
			C13AuxUnusedT.C13AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C13AuxUnusedT.C13AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C13AuxUnusedT _that = getType().cast(o);
		
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
			return "C13AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C13AuxUnusedT  ***********************/
	class C13AuxUnusedTBuilderImpl implements C13AuxUnusedT.C13AuxUnusedTBuilder {
	
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
		public C13AuxUnusedT.C13AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C13AuxUnusedT build() {
			return new C13AuxUnusedT.C13AuxUnusedTImpl(this);
		}
		
		@Override
		public C13AuxUnusedT.C13AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C13AuxUnusedT.C13AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C13AuxUnusedT.C13AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C13AuxUnusedT.C13AuxUnusedTBuilder o = (C13AuxUnusedT.C13AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C13AuxUnusedT _that = getType().cast(o);
		
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
			return "C13AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
