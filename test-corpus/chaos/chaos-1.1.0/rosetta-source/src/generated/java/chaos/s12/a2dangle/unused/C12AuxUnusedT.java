package chaos.s12.a2dangle.unused;

import chaos.s12.a2dangle.unused.meta.C12AuxUnusedTMeta;
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
@RosettaDataType(value="C12AuxUnusedT", builder=C12AuxUnusedT.C12AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C12AuxUnusedT", model="chaos", builder=C12AuxUnusedT.C12AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C12AuxUnusedT extends RosettaModelObject {

	C12AuxUnusedTMeta metaData = new C12AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C12AuxUnusedT build();
	
	C12AuxUnusedT.C12AuxUnusedTBuilder toBuilder();
	
	static C12AuxUnusedT.C12AuxUnusedTBuilder builder() {
		return new C12AuxUnusedT.C12AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C12AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C12AuxUnusedT> getType() {
		return C12AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C12AuxUnusedTBuilder extends C12AuxUnusedT, RosettaModelObjectBuilder {
		C12AuxUnusedT.C12AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C12AuxUnusedT.C12AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C12AuxUnusedT  ***********************/
	class C12AuxUnusedTImpl implements C12AuxUnusedT {
		private final String stub;
		
		protected C12AuxUnusedTImpl(C12AuxUnusedT.C12AuxUnusedTBuilder builder) {
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
		public C12AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C12AuxUnusedT.C12AuxUnusedTBuilder toBuilder() {
			C12AuxUnusedT.C12AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C12AuxUnusedT.C12AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C12AuxUnusedT _that = getType().cast(o);
		
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
			return "C12AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C12AuxUnusedT  ***********************/
	class C12AuxUnusedTBuilderImpl implements C12AuxUnusedT.C12AuxUnusedTBuilder {
	
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
		public C12AuxUnusedT.C12AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C12AuxUnusedT build() {
			return new C12AuxUnusedT.C12AuxUnusedTImpl(this);
		}
		
		@Override
		public C12AuxUnusedT.C12AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C12AuxUnusedT.C12AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C12AuxUnusedT.C12AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C12AuxUnusedT.C12AuxUnusedTBuilder o = (C12AuxUnusedT.C12AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C12AuxUnusedT _that = getType().cast(o);
		
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
			return "C12AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
