package chaos.s34.a2dangle.unused;

import chaos.s34.a2dangle.unused.meta.C34AuxUnusedTMeta;
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
@RosettaDataType(value="C34AuxUnusedT", builder=C34AuxUnusedT.C34AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C34AuxUnusedT", model="chaos", builder=C34AuxUnusedT.C34AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C34AuxUnusedT extends RosettaModelObject {

	C34AuxUnusedTMeta metaData = new C34AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C34AuxUnusedT build();
	
	C34AuxUnusedT.C34AuxUnusedTBuilder toBuilder();
	
	static C34AuxUnusedT.C34AuxUnusedTBuilder builder() {
		return new C34AuxUnusedT.C34AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C34AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C34AuxUnusedT> getType() {
		return C34AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C34AuxUnusedTBuilder extends C34AuxUnusedT, RosettaModelObjectBuilder {
		C34AuxUnusedT.C34AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C34AuxUnusedT.C34AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C34AuxUnusedT  ***********************/
	class C34AuxUnusedTImpl implements C34AuxUnusedT {
		private final String stub;
		
		protected C34AuxUnusedTImpl(C34AuxUnusedT.C34AuxUnusedTBuilder builder) {
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
		public C34AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C34AuxUnusedT.C34AuxUnusedTBuilder toBuilder() {
			C34AuxUnusedT.C34AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C34AuxUnusedT.C34AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34AuxUnusedT _that = getType().cast(o);
		
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
			return "C34AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C34AuxUnusedT  ***********************/
	class C34AuxUnusedTBuilderImpl implements C34AuxUnusedT.C34AuxUnusedTBuilder {
	
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
		public C34AuxUnusedT.C34AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C34AuxUnusedT build() {
			return new C34AuxUnusedT.C34AuxUnusedTImpl(this);
		}
		
		@Override
		public C34AuxUnusedT.C34AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34AuxUnusedT.C34AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34AuxUnusedT.C34AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C34AuxUnusedT.C34AuxUnusedTBuilder o = (C34AuxUnusedT.C34AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34AuxUnusedT _that = getType().cast(o);
		
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
			return "C34AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
