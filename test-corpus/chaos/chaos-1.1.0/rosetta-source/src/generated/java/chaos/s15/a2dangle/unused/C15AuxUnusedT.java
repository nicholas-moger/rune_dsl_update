package chaos.s15.a2dangle.unused;

import chaos.s15.a2dangle.unused.meta.C15AuxUnusedTMeta;
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
@RosettaDataType(value="C15AuxUnusedT", builder=C15AuxUnusedT.C15AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C15AuxUnusedT", model="chaos", builder=C15AuxUnusedT.C15AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C15AuxUnusedT extends RosettaModelObject {

	C15AuxUnusedTMeta metaData = new C15AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C15AuxUnusedT build();
	
	C15AuxUnusedT.C15AuxUnusedTBuilder toBuilder();
	
	static C15AuxUnusedT.C15AuxUnusedTBuilder builder() {
		return new C15AuxUnusedT.C15AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C15AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C15AuxUnusedT> getType() {
		return C15AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C15AuxUnusedTBuilder extends C15AuxUnusedT, RosettaModelObjectBuilder {
		C15AuxUnusedT.C15AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C15AuxUnusedT.C15AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C15AuxUnusedT  ***********************/
	class C15AuxUnusedTImpl implements C15AuxUnusedT {
		private final String stub;
		
		protected C15AuxUnusedTImpl(C15AuxUnusedT.C15AuxUnusedTBuilder builder) {
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
		public C15AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C15AuxUnusedT.C15AuxUnusedTBuilder toBuilder() {
			C15AuxUnusedT.C15AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C15AuxUnusedT.C15AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15AuxUnusedT _that = getType().cast(o);
		
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
			return "C15AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C15AuxUnusedT  ***********************/
	class C15AuxUnusedTBuilderImpl implements C15AuxUnusedT.C15AuxUnusedTBuilder {
	
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
		public C15AuxUnusedT.C15AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C15AuxUnusedT build() {
			return new C15AuxUnusedT.C15AuxUnusedTImpl(this);
		}
		
		@Override
		public C15AuxUnusedT.C15AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15AuxUnusedT.C15AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15AuxUnusedT.C15AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C15AuxUnusedT.C15AuxUnusedTBuilder o = (C15AuxUnusedT.C15AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15AuxUnusedT _that = getType().cast(o);
		
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
			return "C15AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
