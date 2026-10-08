package chaos.s10.a2dangle.unused;

import chaos.s10.a2dangle.unused.meta.C10AuxUnusedTMeta;
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
@RosettaDataType(value="C10AuxUnusedT", builder=C10AuxUnusedT.C10AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C10AuxUnusedT", model="chaos", builder=C10AuxUnusedT.C10AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C10AuxUnusedT extends RosettaModelObject {

	C10AuxUnusedTMeta metaData = new C10AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C10AuxUnusedT build();
	
	C10AuxUnusedT.C10AuxUnusedTBuilder toBuilder();
	
	static C10AuxUnusedT.C10AuxUnusedTBuilder builder() {
		return new C10AuxUnusedT.C10AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C10AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C10AuxUnusedT> getType() {
		return C10AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C10AuxUnusedTBuilder extends C10AuxUnusedT, RosettaModelObjectBuilder {
		C10AuxUnusedT.C10AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C10AuxUnusedT.C10AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C10AuxUnusedT  ***********************/
	class C10AuxUnusedTImpl implements C10AuxUnusedT {
		private final String stub;
		
		protected C10AuxUnusedTImpl(C10AuxUnusedT.C10AuxUnusedTBuilder builder) {
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
		public C10AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C10AuxUnusedT.C10AuxUnusedTBuilder toBuilder() {
			C10AuxUnusedT.C10AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C10AuxUnusedT.C10AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10AuxUnusedT _that = getType().cast(o);
		
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
			return "C10AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C10AuxUnusedT  ***********************/
	class C10AuxUnusedTBuilderImpl implements C10AuxUnusedT.C10AuxUnusedTBuilder {
	
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
		public C10AuxUnusedT.C10AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C10AuxUnusedT build() {
			return new C10AuxUnusedT.C10AuxUnusedTImpl(this);
		}
		
		@Override
		public C10AuxUnusedT.C10AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10AuxUnusedT.C10AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10AuxUnusedT.C10AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C10AuxUnusedT.C10AuxUnusedTBuilder o = (C10AuxUnusedT.C10AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10AuxUnusedT _that = getType().cast(o);
		
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
			return "C10AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
