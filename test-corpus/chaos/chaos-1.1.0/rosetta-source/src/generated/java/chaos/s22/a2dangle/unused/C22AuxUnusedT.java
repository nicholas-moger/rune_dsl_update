package chaos.s22.a2dangle.unused;

import chaos.s22.a2dangle.unused.meta.C22AuxUnusedTMeta;
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
@RosettaDataType(value="C22AuxUnusedT", builder=C22AuxUnusedT.C22AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C22AuxUnusedT", model="chaos", builder=C22AuxUnusedT.C22AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C22AuxUnusedT extends RosettaModelObject {

	C22AuxUnusedTMeta metaData = new C22AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C22AuxUnusedT build();
	
	C22AuxUnusedT.C22AuxUnusedTBuilder toBuilder();
	
	static C22AuxUnusedT.C22AuxUnusedTBuilder builder() {
		return new C22AuxUnusedT.C22AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C22AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C22AuxUnusedT> getType() {
		return C22AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C22AuxUnusedTBuilder extends C22AuxUnusedT, RosettaModelObjectBuilder {
		C22AuxUnusedT.C22AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C22AuxUnusedT.C22AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C22AuxUnusedT  ***********************/
	class C22AuxUnusedTImpl implements C22AuxUnusedT {
		private final String stub;
		
		protected C22AuxUnusedTImpl(C22AuxUnusedT.C22AuxUnusedTBuilder builder) {
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
		public C22AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C22AuxUnusedT.C22AuxUnusedTBuilder toBuilder() {
			C22AuxUnusedT.C22AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C22AuxUnusedT.C22AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22AuxUnusedT _that = getType().cast(o);
		
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
			return "C22AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C22AuxUnusedT  ***********************/
	class C22AuxUnusedTBuilderImpl implements C22AuxUnusedT.C22AuxUnusedTBuilder {
	
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
		public C22AuxUnusedT.C22AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C22AuxUnusedT build() {
			return new C22AuxUnusedT.C22AuxUnusedTImpl(this);
		}
		
		@Override
		public C22AuxUnusedT.C22AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22AuxUnusedT.C22AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22AuxUnusedT.C22AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C22AuxUnusedT.C22AuxUnusedTBuilder o = (C22AuxUnusedT.C22AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22AuxUnusedT _that = getType().cast(o);
		
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
			return "C22AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
