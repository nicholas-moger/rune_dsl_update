package chaos.s14.a2dangle.unused;

import chaos.s14.a2dangle.unused.meta.C14AuxUnusedTMeta;
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
@RosettaDataType(value="C14AuxUnusedT", builder=C14AuxUnusedT.C14AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C14AuxUnusedT", model="chaos", builder=C14AuxUnusedT.C14AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C14AuxUnusedT extends RosettaModelObject {

	C14AuxUnusedTMeta metaData = new C14AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C14AuxUnusedT build();
	
	C14AuxUnusedT.C14AuxUnusedTBuilder toBuilder();
	
	static C14AuxUnusedT.C14AuxUnusedTBuilder builder() {
		return new C14AuxUnusedT.C14AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C14AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C14AuxUnusedT> getType() {
		return C14AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C14AuxUnusedTBuilder extends C14AuxUnusedT, RosettaModelObjectBuilder {
		C14AuxUnusedT.C14AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C14AuxUnusedT.C14AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C14AuxUnusedT  ***********************/
	class C14AuxUnusedTImpl implements C14AuxUnusedT {
		private final String stub;
		
		protected C14AuxUnusedTImpl(C14AuxUnusedT.C14AuxUnusedTBuilder builder) {
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
		public C14AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C14AuxUnusedT.C14AuxUnusedTBuilder toBuilder() {
			C14AuxUnusedT.C14AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C14AuxUnusedT.C14AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14AuxUnusedT _that = getType().cast(o);
		
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
			return "C14AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C14AuxUnusedT  ***********************/
	class C14AuxUnusedTBuilderImpl implements C14AuxUnusedT.C14AuxUnusedTBuilder {
	
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
		public C14AuxUnusedT.C14AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C14AuxUnusedT build() {
			return new C14AuxUnusedT.C14AuxUnusedTImpl(this);
		}
		
		@Override
		public C14AuxUnusedT.C14AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14AuxUnusedT.C14AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14AuxUnusedT.C14AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C14AuxUnusedT.C14AuxUnusedTBuilder o = (C14AuxUnusedT.C14AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14AuxUnusedT _that = getType().cast(o);
		
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
			return "C14AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
